package com.stocktrade.matchingsystem;

import com.stocktrade.matchingsystem.common.model.dto.OrderRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiControllerIntegrationTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerLoginAndOrderAuthControlWorks() {
        ResponseEntity<Map<String, Object>> reg = register("u_auth_1", "p12345");
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = (String) reg.getBody().get("token");
        assertThat(token).isNotBlank();
        String registeredShareholder = (String) reg.getBody().get("shareHolderId");
        assertThat(registeredShareholder).startsWith("SH_");

        ResponseEntity<Map<String, Object>> dup = register("u_auth_1", "p12345");
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, Object>> login = login("u_auth_1", "p12345");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderRequestDTO req = order("XSHG", "600000", "B", 100, 10.0);
        ResponseEntity<Map<String, Object>> unauthSubmit = rest.postForEntity("/api/orders", req, mapType());
        assertThat(unauthSubmit.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, Object>> submit = rest.exchange(
                "/api/orders",
                HttpMethod.POST,
                authed(req, token),
                mapType());
        assertThat(submit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submit.getBody().get("clOrderId")).isNotNull();

        ResponseEntity<List<Map<String, Object>>> ownOrders = rest.exchange(
                "/api/orders",
                HttpMethod.GET,
                authed(null, token),
                listMapType());
        assertThat(ownOrders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownOrders.getBody()).hasSize(1);
        assertThat((String) ownOrders.getBody().get(0).get("shareHolderId")).isEqualTo(registeredShareholder);
    }

    @Test
    void multiUserMultiPriceMultiQtyFlowMatchesExpectedByRest() {
        String s1Token = register("u_s1", "p12345").getBody().get("token").toString();
        String s2Token = register("u_s2", "p12345").getBody().get("token").toString();
        String s3Token = register("u_s3", "p12345").getBody().get("token").toString();
        String b1Token = register("u_b1", "p12345").getBody().get("token").toString();
        String adminToken = login("admin", "admin123").getBody().get("token").toString();

        String s1Id = submitOrderAndGetId(s1Token, order("XSHG", "600000", "S", 200, 10.0));
        String s2Id = submitOrderAndGetId(s2Token, order("XSHG", "600000", "S", 100, 9.8));
        String s3Id = submitOrderAndGetId(s3Token, order("XSHG", "600000", "S", 100, 10.0));
        String b1Id = submitOrderAndGetId(b1Token, order("XSHG", "600000", "B", 500, 10.0));

        ResponseEntity<List<Map<String, Object>>> executionsResp = rest.exchange(
                "/api/admin/executions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                listMapType());
        assertThat(executionsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> executions = executionsResp.getBody().stream()
                .filter(e -> "600000".equals(e.get("securityId")))
                .sorted(Comparator.comparingLong(e -> ((Number) e.get("id")).longValue()))
                .toList();
        assertThat(executions).hasSize(6);
        assertThat(executions).extracting(e -> e.get("execType")).containsOnly("INTERNAL");

        List<Map<String, Object>> buyExecs = executions.stream()
                .filter(e -> b1Id.equals(e.get("clOrderId")))
                .toList();
        assertThat(buyExecs).hasSize(3);
        assertThat(buyExecs).extracting(e -> ((Number) e.get("execQty")).intValue()).containsExactly(100, 200, 100);
        assertThat(buyExecs).extracting(e -> ((Number) e.get("execPrice")).doubleValue()).containsExactly(9.8, 10.0, 10.0);

        List<Map<String, Object>> sellExecs = executions.stream()
                .filter(e -> !b1Id.equals(e.get("clOrderId")))
                .toList();
        assertThat(sellExecs).extracting(e -> e.get("clOrderId")).containsExactly(s2Id, s1Id, s3Id);

        ResponseEntity<List<Map<String, Object>>> ordersResp = rest.exchange(
                "/api/orders",
                HttpMethod.GET,
                authed(null, adminToken),
                listMapType());
        assertThat(ordersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Map<String, Object>> byId = ordersResp.getBody().stream()
                .collect(java.util.stream.Collectors.toMap(o -> o.get("clOrderId").toString(), o -> o));

        assertThat(byId.get(b1Id).get("status")).isEqualTo("EXCH_WORKING");
        assertThat(((Number) byId.get(b1Id).get("qtyFilled")).intValue()).isEqualTo(400);
        assertThat(((Number) byId.get(b1Id).get("qtyRemaining")).intValue()).isEqualTo(100);

        assertThat(byId.get(s1Id).get("status")).isEqualTo("CANCELED");
        assertThat(byId.get(s2Id).get("status")).isEqualTo("CANCELED");
        assertThat(byId.get(s3Id).get("status")).isEqualTo("CANCELED");

        ResponseEntity<List<Map<String, Object>>> s1View = rest.exchange(
                "/api/orders",
                HttpMethod.GET,
                authed(null, s1Token),
                listMapType());
        assertThat(s1View.getBody()).hasSize(1);
        assertThat(s1View.getBody().get(0).get("clOrderId")).isEqualTo(s1Id);
    }

    @Test
    void marketSnapshotAndMyStockActivityEndpointsWork() {
        String sellerToken = register("u_mkt_seller", "p12345").getBody().get("token").toString();
        String buyerToken = register("u_mkt_buyer", "p12345").getBody().get("token").toString();

        String sellId = submitOrderAndGetId(sellerToken, order("XSHG", "600001", "S", 100, 10.0));
        String buyId = submitOrderAndGetId(buyerToken, order("XSHG", "600001", "B", 100, 10.0));

        ResponseEntity<List<Map<String, Object>>> marketResp = rest.exchange(
                "/api/market/recent-executions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                listMapType());
        assertThat(marketResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> row = marketResp.getBody().stream()
                .filter(it -> "XSHG".equals(it.get("market")) && "600001".equals(it.get("securityId")))
                .findFirst()
                .orElseThrow();
        assertThat(((Number) row.get("execQty")).intValue()).isEqualTo(100);
        assertThat(((Number) row.get("execPrice")).doubleValue()).isEqualTo(10.0);
        assertThat(((Number) row.get("executionCount")).longValue()).isGreaterThanOrEqualTo(1L);

        ResponseEntity<Map<String, Object>> unauthorized = rest.exchange(
                "/api/me/stocks/activity",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                mapType());
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<List<Map<String, Object>>> sellerActivity = rest.exchange(
                "/api/me/stocks/activity",
                HttpMethod.GET,
                authed(null, sellerToken),
                listMapType());
        assertThat(sellerActivity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> sellerStock = sellerActivity.getBody().stream()
                .filter(it -> "XSHG".equals(it.get("market")) && "600001".equals(it.get("securityId")))
                .findFirst()
                .orElseThrow();

        List<Map<String, Object>> orders = (List<Map<String, Object>>) sellerStock.get("orders");
        assertThat(orders).extracting(it -> it.get("clOrderId")).contains(sellId).doesNotContain(buyId);

        List<Map<String, Object>> reports = (List<Map<String, Object>>) sellerStock.get("reports");
        assertThat(reports).extracting(it -> it.get("type")).contains("CONFIRM", "EXECUTION");
    }

    @Test
    void myStockActivitySupportsMarketAndSecurityFilters() {
        String userToken = register("u_filter_1", "p12345").getBody().get("token").toString();
        String counterToken = register("u_filter_2", "p12345").getBody().get("token").toString();

        submitOrderAndGetId(userToken, order("XSHG", "600001", "S", 100, 10.0));
        submitOrderAndGetId(userToken, order("XSHE", "000001", "B", 100, 10.0));
        submitOrderAndGetId(counterToken, order("XSHG", "600001", "B", 100, 10.0));

        ResponseEntity<List<Map<String, Object>>> onlyXshg = rest.exchange(
                "/api/me/stocks/activity?market=XSHG",
                HttpMethod.GET,
                authed(null, userToken),
                listMapType());
        assertThat(onlyXshg.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(onlyXshg.getBody()).extracting(it -> it.get("market")).containsOnly("XSHG");

        ResponseEntity<List<Map<String, Object>>> onlySecurity = rest.exchange(
                "/api/me/stocks/activity?securityId=000001",
                HttpMethod.GET,
                authed(null, userToken),
                listMapType());
        assertThat(onlySecurity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(onlySecurity.getBody()).extracting(it -> it.get("securityId")).containsOnly("000001");
    }

    @Test
    void myOrdersAndOrderExecutionDetailEndpointsWork() {
        String sellerToken = register("u_me_seller", "p12345").getBody().get("token").toString();
        String buyerToken = register("u_me_buyer", "p12345").getBody().get("token").toString();

        String sellId = submitOrderAndGetId(sellerToken, order("XSHG", "600519", "S", 100, 10.0));
        submitOrderAndGetId(buyerToken, order("XSHG", "600519", "B", 100, 10.0));

        ResponseEntity<List<Map<String, Object>>> myOrdersResp = rest.exchange(
                "/api/me/orders?market=XSHG&securityId=600519",
                HttpMethod.GET,
                authed(null, sellerToken),
                listMapType());
        assertThat(myOrdersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(myOrdersResp.getBody()).hasSize(1);
        Map<String, Object> one = myOrdersResp.getBody().get(0);
        assertThat(one.get("clOrderId")).isEqualTo(sellId);
        assertThat(one.get("market")).isEqualTo("XSHG");
        assertThat(one.get("securityId")).isEqualTo("600519");

        ResponseEntity<Map<String, Object>> detailResp = rest.exchange(
                "/api/me/orders/" + sellId + "/executions",
                HttpMethod.GET,
                authed(null, sellerToken),
                mapType());
        assertThat(detailResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResp.getBody().get("clOrderId")).isEqualTo(sellId);
        assertThat(((Number) detailResp.getBody().get("executionCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) detailResp.getBody().get("totalExecutedQty")).intValue()).isEqualTo(100);

        ResponseEntity<Map<String, Object>> forbiddenView = rest.exchange(
                "/api/me/orders/" + sellId + "/executions",
                HttpMethod.GET,
                authed(null, buyerToken),
                mapType());
        assertThat(forbiddenView.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void validationGuardsAuthAndOrderRequests() {
        Map<String, Object> invalidRegister = Map.of(
                "loginId", "user_short_pw",
                "password", "123");
        ResponseEntity<Map<String, Object>> invalidRegResp = rest.postForEntity(
                "/api/auth/register",
                invalidRegister,
                mapType());
        assertThat(invalidRegResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String token = register("u_valid_1", "p12345").getBody().get("token").toString();
        OrderRequestDTO invalidOrder = order("XSHG", "12", "B", 100, 10.0);
        ResponseEntity<Map<String, Object>> invalidOrderResp = rest.exchange(
                "/api/orders",
                HttpMethod.POST,
                authed(invalidOrder, token),
                mapType());
        assertThat(invalidOrderResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> register(String loginId, String password) {
        Map<String, Object> body = Map.of(
                "loginId", loginId,
                "password", password);
        return rest.postForEntity("/api/auth/register", body, mapType());
    }

    private ResponseEntity<Map<String, Object>> login(String loginId, String password) {
        Map<String, Object> body = Map.of(
                "loginId", loginId,
                "password", password);
        return rest.postForEntity("/api/auth/login", body, mapType());
    }

    private String submitOrderAndGetId(String token, OrderRequestDTO req) {
        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "/api/orders",
                HttpMethod.POST,
                authed(req, token),
                mapType());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("resultType")).isEqualTo("ACCEPTED");
        return resp.getBody().get("clOrderId").toString();
    }

    private OrderRequestDTO order(String market, String sec, String side, int qty, double price) {
        OrderRequestDTO req = new OrderRequestDTO();
        req.setMarket(market);
        req.setSecurityId(sec);
        req.setSide(side);
        req.setQty(qty);
        req.setPrice(price);
        return req;
    }

    private HttpEntity<?> authed(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Class<Map<String, Object>> mapType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    private ParameterizedTypeReference<List<Map<String, Object>>> listMapType() {
        return new ParameterizedTypeReference<>() {
        };
    }
}
