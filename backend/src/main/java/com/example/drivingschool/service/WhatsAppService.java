package com.example.drivingschool.service;



import com.example.drivingschool.model.Customer;
import com.example.drivingschool.model.Schedule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class WhatsAppService {
    RestTemplate restTemplate = new RestTemplate();
    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    public String sendTemplateMessage(String phoneNumber) {

        String url = apiUrl + phoneNumberId + "/messages";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Sending the default "hello_world" template to test number
        String body = """
            {
              "messaging_product": "whatsapp",
              "to": "%s",
              "type": "template",
              "template": {
                "name": "hello_world",
                "language": { "code": "en_US" }
              }
            }
        """.formatted(phoneNumber);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        return response.getBody();
    }

    public void sendTextMessage(String contact, String message) {
        try {
            String url = apiUrl + phoneNumberId + "/messages";
            System.out.println("WhatsApp API URL: " + url);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            System.out.println("Authorization Token: " + token);

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode body = mapper.createObjectNode();
            body.put("messaging_product", "whatsapp");
            body.put("to", contact);
            body.put("type", "text");

            ObjectNode textNode = mapper.createObjectNode();
            textNode.put("body", message);

            body.set("text", textNode);

            String requestBody = mapper.writeValueAsString(body);
            System.out.println("Request JSON: " + requestBody);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to send WhatsApp message: " + ex.getMessage());
        }
    }


    public void sendEnrollmentTemplate(String contact, Customer customer) {
        try {
            // 1. API Endpoint & Token Setup
            String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

            // 2. Formatting Date and Time for a professional look
            String readableDate = customer.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM, yyyy"));
            String readableTime = customer.getPreferredStartTime().format(DateTimeFormatter.ofPattern("hh:mm a"));

            // 3. Main Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", contact);
            payload.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", "customer_enrollment_success"); // Use the NEW name here



            Map<String, String> language = new HashMap<>();
            language.put("code", "en");
            template.put("language", language);

            // 4. Mapping EXACTLY 6 Parameters
            List<Map<String, String>> parameters = new ArrayList<>();

            parameters.add(Map.of("type", "text", "text", customer.getName())); // {{1}}
            parameters.add(Map.of("type", "text", "text", customer.getCourse().getCourseName())); // {{2}}
            parameters.add(Map.of("type", "text", "text", customer.getStartDate().toString())); // {{3}}
            parameters.add(Map.of("type", "text", "text", customer.getPreferredStartTime().toString())); // {{4}}
            parameters.add(Map.of("type", "text", "text", customer.getAssignedInstructor().getName())); // {{5}}

            // Handling instructor contact (Ensure your Instructor entity has a contact field)
            String instructorPhone = customer.getAssignedInstructor().getContact() != null ?
                    customer.getAssignedInstructor().getContact() : "Office Support";
            parameters.add(Map.of("type", "text", "text", instructorPhone));                    // {{6}}

            // 5. Wrap Components
            template.put("components", List.of(
                    Map.of("type", "body", "parameters", parameters)
            ));

            payload.put("template", template);

            // 6. Set Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            // 7. Execute POST
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            System.out.println("WhatsApp Notification Sent: " + response.getBody());

        } catch (Exception e) {
            System.err.println("WhatsApp Integration Error: " + e.getMessage());
            // Detailed error log
            if (e.getMessage().contains("400")) {
                System.err.println("Check: Does the template 'driving_enrollment_pro_v1' have exactly 6 variables?");
            }
        }
    }


    public void sendJasperTestTemplate(String recipientNumber, String customerName) {
        String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";



        // 1. Build the Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "jaspers_market_order_confirmation_v1");
        template.put("language", Map.of("code", "en"));

        // 2. Prepare 3 Parameters (to match the template requirement)
        List<Map<String, Object>> parameters = new ArrayList<>();

        // {{1}} - Customer Name
        parameters.add(Map.of("type", "text", "text", customerName));

        // {{2}} - Order/Enrollment ID
        parameters.add(Map.of("type", "text", "text", "ENROLL-" + System.currentTimeMillis() / 100000));

        // {{3}} - Date or Additional Detail (The missing one!)
        parameters.add(Map.of("type", "text", "text", "Today"));

        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        payload.put("template", template);

        // 3. Set Headers & Send
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("Success! Message ID: " + response.getBody());
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
        }
    }


    public void sendSessionCompletionMessage(String recipientNumber, Schedule schedule) {
        String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "session_completion_utility");
        template.put("language", Map.of("code", "en")); // Use en_US to be safe

        List<Map<String, Object>> parameters = new ArrayList<>();

        // {{1}} - Customer Name (THIS WAS MISSING)
        parameters.add(Map.of("type", "text", "text", schedule.getCustomer().getName()));

        // {{2}} - Session Date
        parameters.add(Map.of("type", "text", "text", schedule.getDate().toString()));

        // {{3}} - Session Time
        parameters.add(Map.of("type", "text", "text", schedule.getStartTime().toString()));

        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        payload.put("template", template);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            System.err.println("WhatsApp API Error: " + e.getMessage());
        }
    }
    public void sendRescheduleMessage(String recipientNumber, String customerName, String oldDate, String oldTime, String newDate, String newTime) {
        // Ensure the number is formatted correctly (Meta expects digits only)
        String formattedNumber = recipientNumber.replace("+", "").trim();
        String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", formattedNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "session_rescheduled_notice");
        template.put("language", Map.of("code", "en"));

        // Prepare the 5 Parameters
        // IMPORTANT: Meta's API will reject if any of these strings are null
        List<Map<String, Object>> parameters = List.of(
                Map.of("type", "text", "text", Objects.requireNonNullElse(customerName, "Customer")),
                Map.of("type", "text", "text", Objects.requireNonNullElse(oldDate, "N/A")),
                Map.of("type", "text", "text", Objects.requireNonNullElse(oldTime, "N/A")),
                Map.of("type", "text", "text", Objects.requireNonNullElse(newDate, "N/A")),
                Map.of("type", "text", "text", Objects.requireNonNullElse(newTime, "N/A"))
        );

        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        payload.put("template", template);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token); // Ensure 'token' is your valid Access Token

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("WhatsApp Notification Sent Status: " + response.getStatusCode());
        } catch (Exception e) {
            // Log the error but don't crash the main business logic
            System.err.println("Error notifying customer via WhatsApp: " + e.getMessage());
        }
    }

    public void sendCancellationMessage(String recipientNumber, String customerName, String date, String time) {
        String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "session_cancellation_alert");
        template.put("language", Map.of("code", "en"));

        // Prepare 3 Parameters
        List<Map<String, Object>> parameters = List.of(
                Map.of("type", "text", "text", customerName), // {{1}}
                Map.of("type", "text", "text", date),         // {{2}}
                Map.of("type", "text", "text", time)          // {{3}}
        );

        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        payload.put("template", template);

        // Set Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Execute Request
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling WhatsApp API: " + e.getMessage());
        }
    }

    public void sendBulkCourseUpdate(String recipientNumber, String customerName, String newTimeSlot) {
        String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "bulk_time_update"); // Must match dashboard name
        template.put("language", Map.of("code", "en"));

        // Prepare 2 Parameters
        List<Map<String, Object>> parameters = List.of(
                Map.of("type", "text", "text", customerName), // {{1}}
                Map.of("type", "text", "text", newTimeSlot)   // {{2}}
        );

        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        payload.put("template", template);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("Bulk message sent to " + customerName);
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
        }
    }

    public void sendPaymentReminder(String recipientPhone, String name, String pendingAmount, String totalAmount, LocalDate date) {
        String url = apiUrl + phoneNumberId + "/messages";

        // 1. Construct Request Body
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientPhone);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        //payment_reminder_utility

        template.put("name", "payment_utility_template");
        template.put("language", Map.of("code", "en"));

        // 2. Prepare 3 Parameters
        List<Map<String, String>> parameters = List.of(
                Map.of("type", "text", "text", name),           // {{1}}
                Map.of("type", "text", "text", pendingAmount),  // {{2}}
                Map.of("type", "text", "text", totalAmount),
                Map.of("type", "text", "text", date.toString())
        );

        template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        payload.put("template", template);

        // 3. Set Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token); // Using 'token' variable as per your class

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            // Using System.out (Equivalent to console.log)
            System.out.println("WhatsApp Reminder Sent Successfully: " + response.getBody());
        } catch (Exception e) {
            // Using System.err (Equivalent to console.error)
            System.err.println("Failed to send WhatsApp reminder: " + e.getMessage());
        }
    }

    public void sendFeedbackMessage(String phoneNumber, String customerName, LocalDate endDate) {
        String url = apiUrl + phoneNumberId + "/messages";

        // Format the date for the message (e.g., 28 Dec 2025)
        String formattedDate = endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        // Map variables to {{1}} and {{2}}
        List<Map<String, Object>> parameters = new ArrayList<>();
        parameters.add(Map.of("type", "text", "text", customerName));  // {{1}}
        parameters.add(Map.of("type", "text", "text", formattedDate)); // {{2}}

        Map<String, Object> template = new HashMap<>();
        template.put("name", "service_feedback_v1"); // Your template name
        template.put("language", Map.of("code", "en_US"));
        template.put("components", List.of(
                Map.of("type", "body", "parameters", parameters)
        ));

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", phoneNumber);
        body.put("type", "template");
        body.put("template", template);

        executeRequest(url, body);
    }

    private void executeRequest(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            System.err.println("WhatsApp API Error: " + e.getMessage());
        }
    }
}
