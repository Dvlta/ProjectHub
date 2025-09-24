package com.leorsun.projecthub.controller;


import com.leorsun.projecthub.dto.LoginUserDto;
import com.leorsun.projecthub.dto.RegisterUserDto;
import com.leorsun.projecthub.dto.VerifyUserDto;
import com.leorsun.projecthub.model.User;
import com.leorsun.projecthub.responses.LoginResponse;
import com.leorsun.projecthub.service.AuthenticationService;
import com.leorsun.projecthub.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto){
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime(), refreshToken, jwtService.getRefreshExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody com.leorsun.projecthub.dto.RefreshTokenRequest body) {
        String refreshToken = body.getRefreshToken();
        // Basic validation without persistence: verify type and expiry, then issue a new access token
        if (!jwtService.isRefreshToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }
        String subject = jwtService.extractUsername(refreshToken);
        User user = authenticationService.loadUserByEmail(subject);
        String newAccess = jwtService.generateToken(user);
        LoginResponse response = new LoginResponse(newAccess, jwtService.getExpirationTime(), refreshToken, jwtService.getRefreshExpirationTime());
        return ResponseEntity.ok(response);
    }
}
