package com.harumi.petrecord.dailyrecord;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/truncate-all.sql", executionPhase = BEFORE_TEST_METHOD)
class DailyRecordFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerToken(String username) throws Exception {
        String body = String.format("""
                {"username":"%s","email":"%s@example.com","password":"password123"}
                """, username, username);
        MvcResult r = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsByteArray()).get("accessToken").asText();
    }

    private long createPet(String token) throws Exception {
        MvcResult r = mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mochi\",\"species\":\"CAT\"}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsByteArray()).get("id").asLong();
    }

    @Test
    void fullDailyRecordFlow() throws Exception {
        String token = registerToken("alice");
        long petId = createPet(token);
        String base = "/api/pets/" + petId + "/daily-records";

        String createBody = """
                {
                  "recordDate": "2026-05-25",
                  "weightKg": 5.20,
                  "waterMl": 300,
                  "dailyNote": "good day",
                  "feedings": [{"feedingTime":"08:00:00","foodGram":120,"conditionText":"dry"}],
                  "stools": [{"stoolTime":"09:00:00","conditionText":"normal","abnormal":false}]
                }
                """;
        MvcResult created = mvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.weightKg").value(5.20))
                .andExpect(jsonPath("$.feedings.length()").value(1))
                .andExpect(jsonPath("$.stools[0].abnormal").value(false))
                .andReturn();
        long recordId = mapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();

        // list
        mvc.perform(get(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // list by date
        mvc.perform(get(base + "?date=2026-05-25").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(base + "?date=2000-01-01").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // get by id
        mvc.perform(get(base + "/" + recordId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedings.length()").value(1));

        // PUT replace children (2 feedings, 0 stools)
        String updateBody = """
                {
                  "weightKg": 6.00,
                  "waterMl": 350,
                  "dailyNote": "updated",
                  "feedings": [
                    {"feedingTime":"18:00:00","foodGram":200},
                    {"feedingTime":"20:00:00","foodGram":50}
                  ],
                  "stools": []
                }
                """;
        mvc.perform(put(base + "/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(6.00))
                .andExpect(jsonPath("$.feedings.length()").value(2))
                .andExpect(jsonPath("$.stools.length()").value(0));

        // duplicate date -> 409
        mvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"2026-05-25\"}"))
                .andExpect(status().isConflict());

        // delete -> 204, then 404
        mvc.perform(delete(base + "/" + recordId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get(base + "/" + recordId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossUserCannotAccessOthersPetRecords() throws Exception {
        String alice = registerToken("alice2");
        String bob = registerToken("bob2");
        long alicePet = createPet(alice);
        String base = "/api/pets/" + alicePet + "/daily-records";

        // Bob tries to create / list on Alice's pet -> 404 (pet not owned)
        mvc.perform(post(base)
                        .header("Authorization", "Bearer " + bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"2026-05-25\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(base).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationAndAuthErrors() throws Exception {
        String token = registerToken("alice3");
        long petId = createPet(token);
        String base = "/api/pets/" + petId + "/daily-records";

        // missing recordDate -> 400
        mvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // future date -> 400
        mvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"2099-01-01\"}"))
                .andExpect(status().isBadRequest());

        // unauthenticated -> 401
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
    }
}
