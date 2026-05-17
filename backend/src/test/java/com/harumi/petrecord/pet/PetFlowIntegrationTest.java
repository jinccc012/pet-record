package com.harumi.petrecord.pet;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/truncate-all.sql", executionPhase = BEFORE_TEST_METHOD)
class PetFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void fullPetCrudFlow() throws Exception {
        String token = registerAndGetToken("alice", "alice@example.com");

        // Create
        String createBody = """
                {"name":"Mochi","species":"CAT","breed":"British Shorthair",
                 "gender":"FEMALE","birthDate":"2022-03-01","color":"grey","notes":"shy"}
                """;
        MvcResult created = mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Mochi"))
                .andExpect(jsonPath("$.species").value("CAT"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn();

        Long petId = mapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();

        // List
        mvc.perform(get("/api/pets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(petId))
                .andExpect(jsonPath("$[0].name").value("Mochi"));

        // Get
        mvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mochi"));

        // Patch (only name)
        String patchBody = """
                {"name":"Mochi II"}
                """;
        mvc.perform(patch("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mochi II"))
                .andExpect(jsonPath("$.color").value("grey"));

        // Delete
        mvc.perform(delete("/api/pets/" + petId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // After delete: GET should 404
        mvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossUserAccessReturns404() throws Exception {
        String aliceToken = registerAndGetToken("alice2", "alice2@example.com");
        String bobToken = registerAndGetToken("bob2", "bob2@example.com");

        String createBody = """
                {"name":"Mochi","species":"CAT"}
                """;
        MvcResult created = mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        Long petId = mapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();

        // Bob cannot GET Alice's pet
        mvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Bob cannot PATCH Alice's pet
        mvc.perform(patch("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isNotFound());

        // Bob cannot DELETE Alice's pet
        mvc.perform(delete("/api/pets/" + petId).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Bob's list does not contain Alice's pet
        mvc.perform(get("/api/pets").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mvc.perform(get("/api/pets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"species\":\"CAT\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        String token = registerAndGetToken("alice3", "alice3@example.com");

        // missing name + species
        mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // birthDate in the future
        String futureBody = """
                {"name":"Future","species":"DOG","birthDate":"2099-01-01"}
                """;
        mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureBody))
                .andExpect(status().isBadRequest());

        // invalid species enum
        String badEnumBody = """
                {"name":"Alien","species":"DRAGON"}
                """;
        mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badEnumBody))
                .andExpect(status().isBadRequest());
    }

    private String registerAndGetToken(String username, String email) throws Exception {
        String body = String.format("""
                {"username":"%s","email":"%s","password":"password123"}
                """, username, email);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = mapper.readTree(result.getResponse().getContentAsByteArray());
        return json.get("accessToken").asText();
    }
}
