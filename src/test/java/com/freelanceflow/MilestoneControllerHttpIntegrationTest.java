package com.freelanceflow;

import com.freelanceflow.controller.MilestoneController;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.GlobalExceptionHandler;
import com.freelanceflow.exception.IllegalStateTransitionException;
import com.freelanceflow.exception.InsufficientFundsException;
import com.freelanceflow.model.Milestone;
import com.freelanceflow.model.MilestoneStatus;
import com.freelanceflow.model.Role;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.MilestoneStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MilestoneControllerHttpIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private MilestoneStateMachine milestoneStateMachine;

    @InjectMocks
    private MilestoneController milestoneController;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal("user-1", "user@test.com", "pass", Role.CLIENT, false, Collections.emptyList());

        mockMvc = MockMvcBuilders.standaloneSetup(milestoneController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HTTP 200 OK: Valid fund milestone")
    void testFundMilestoneSuccess() throws Exception {
        Milestone m = Milestone.builder()
                .id("m-1")
                .contractId("c-1")
                .title("Milestone 1")
                .amount(BigDecimal.valueOf(250.00))
                .status(MilestoneStatus.FUNDED)
                .build();

        when(milestoneStateMachine.fundMilestone(eq("user-1"), eq("m-1"))).thenReturn(m);

        mockMvc.perform(post("/api/milestones/m-1/fund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FUNDED"));
    }

    @Test
    @DisplayName("HTTP 409 Conflict: Illegal state transition")
    void testIllegalStateTransitionHttp409() throws Exception {
        when(milestoneStateMachine.fundMilestone(eq("user-1"), eq("m-1")))
                .thenThrow(new IllegalStateTransitionException("Illegal transition"));

        mockMvc.perform(post("/api/milestones/m-1/fund"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("HTTP 403 Forbidden: Wrong role access")
    void testWrongRoleHttp403() throws Exception {
        when(milestoneStateMachine.fundMilestone(eq("user-1"), eq("m-1")))
                .thenThrow(new ForbiddenException("Only client can fund milestone"));

        mockMvc.perform(post("/api/milestones/m-1/fund"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("HTTP 400 Bad Request: Insufficient funds")
    void testInsufficientFundsHttp400() throws Exception {
        when(milestoneStateMachine.fundMilestone(eq("user-1"), eq("m-1")))
                .thenThrow(new InsufficientFundsException("Insufficient wallet balance"));

        mockMvc.perform(post("/api/milestones/m-1/fund"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
