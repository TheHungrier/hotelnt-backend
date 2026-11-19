package com.hotelnt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleDistanceServiceTest {

  private final ObjectMapper json = new ObjectMapper();

  @Test
  void nullCoordinatesReturnNull() {
    GoogleDistanceService service = new GoogleDistanceService(json);
    assertNull(service.calculateDistanceKm(null, 109.19, 12.25, 109.20));
    assertNull(service.calculateDistanceKm(12.24, null, 12.25, 109.20));
    assertNull(service.calculateDistanceKm(12.24, 109.19, null, 109.20));
    assertNull(service.calculateDistanceKm(12.24, 109.19, 12.25, null));
  }

  @Test
  void samePointReturnsZeroImmediately() {
    GoogleDistanceService service = new GoogleDistanceService(json);
    assertEquals(0.0, service.calculateDistanceKm(12.245, 109.195, 12.245, 109.195));
  }

  @Test
  void usesGoogleMapsDistanceWhenAvailable() throws Exception {
    HttpClient mockClient = Mockito.mock(HttpClient.class);
    HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);

    String googleJson = """
        {
          "status": "OK",
          "rows": [
            {
              "elements": [
                {
                  "status": "OK",
                  "distance": { "value": 3540, "text": "3.5 km" },
                  "duration": { "value": 480, "text": "8 mins" }
                }
              ]
            }
          ]
        }
        """;

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(googleJson);
    when(mockClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(mockResponse);

    GoogleDistanceService service = new GoogleDistanceService(json, mockClient, "valid-fake-key");
    Double dist = service.calculateDistanceKm(12.24, 109.19, 12.27, 109.21);

    assertEquals(3.54, dist);
    verify(mockClient, times(1)).send(any(), any());

    // Lan goi thu 2 voi cung toa do -> lay tu cache, khong goi lai HTTP Client
    Double cachedDist = service.calculateDistanceKm(12.24, 109.19, 12.27, 109.21);
    assertEquals(3.54, cachedDist);
    verify(mockClient, times(1)).send(any(), any());
  }

  @Test
  void fallsBackToHaversineWhenApiKeyMissing() {
    GoogleDistanceService service = new GoogleDistanceService(json, null, "");
    Double dist = service.calculateDistanceKm(12.0, 109.0, 13.0, 109.0);
    assertNotNull(dist);
    // Haversine khoang cach 1 do vi do ~ 111.19 km
    assertEquals(111.19, dist, 0.1);
  }

  @Test
  void fallsBackToHaversineWhenApiReturnsError() throws Exception {
    HttpClient mockClient = Mockito.mock(HttpClient.class);
    HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(500);
    when(mockClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(mockResponse);

    GoogleDistanceService service = new GoogleDistanceService(json, mockClient, "valid-fake-key");
    Double dist = service.calculateDistanceKm(12.0, 109.0, 13.0, 109.0);

    assertNotNull(dist);
    assertEquals(111.19, dist, 0.1);
  }
}