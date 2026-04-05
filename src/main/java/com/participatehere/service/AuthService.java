package com.participatehere.service;

import com.participatehere.dto.request.LoginRequest;
import com.participatehere.dto.request.RegisterRequest;
import com.participatehere.dto.response.AuthResponse;
import com.participatehere.entity.Admin;
import com.participatehere.entity.RefreshToken;
import com.participatehere.entity.User;
import com.participatehere.exception.BadRequestException;
import com.participatehere.repository.AdminRepository;
import com.participatehere.repository.UserRepository;
import com.participatehere.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Register ──────────────────────────────────────────────────────────────
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setStudentId(request.getStudentId());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.STUDENT);

        User saved = userRepository.save(user);

        try {
            String welcomeBody =
                "<div style='font-family: Arial, sans-serif; max-width: 620px; margin: auto; padding: 0; border-radius: 10px; overflow: hidden; border: 1px solid #e0e0e0;'>" +
                "  <div style='background: linear-gradient(135deg, #1a73e8, #0d47a1); padding: 40px 30px; text-align: center;'>" +
                "    <h1 style='color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px;'>🎉 Welcome to ParticipateHere!</h1>" +
                "    <p style='color: #c9d8ff; margin-top: 8px; font-size: 14px;'>Your account has been created successfully</p>" +
                "  </div>" +
                "  <div style='padding: 35px 30px; background: #ffffff;'>" +
                "    <p style='color: #333; font-size: 16px;'>Hello <strong>" + saved.getFirstName() + " " + saved.getLastName() + "</strong>,</p>" +
                "    <p style='color: #555; font-size: 15px; line-height: 1.7;'>" +
                "      We're thrilled to have you on board! Your <strong>ParticipateHere</strong> account has been successfully created. " +
                "      You can now log in and start exploring everything our platform has to offer." +
                "    </p>" +
                "    <table style='width:100%; background:#f4f8ff; border-radius:8px; padding:15px; margin: 25px 0; border-left: 4px solid #1a73e8;'>" +
                "      <tr><td style='color:#888; padding:5px 10px; font-size:14px;'>📧 Registered Email</td><td style='color:#333; font-weight:bold; font-size:14px;'>" + saved.getEmail() + "</td></tr>" +
                "      <tr><td style='color:#888; padding:5px 10px; font-size:14px;'>🎓 Student ID</td><td style='color:#333; font-weight:bold; font-size:14px;'>" + saved.getStudentId() + "</td></tr>" +
                "    </table>" +
                "    <div style='text-align:center; margin: 30px 0;'>" +
                "      <a href='https://participatehere.vercel.app/login' style='background: #1a73e8; color: #fff; padding: 14px 32px; border-radius: 6px; text-decoration: none; font-size: 15px; font-weight: bold;'>Login to Your Account →</a>" +
                "    </div>" +
                "    <p style='color:#777; font-size:13px;'>If you did not create this account, please ignore this email or contact our support team.</p>" +
                "  </div>" +
                "  <div style='background:#f5f5f5; padding:18px; text-align:center; border-top:1px solid #e0e0e0;'>" +
                "    <p style='color:#aaa; font-size:12px; margin:0;'>© 2025 ParticipateHere. All rights reserved. | This is an automated email, please do not reply.</p>" +
                "  </div>" +
                "</div>";
            emailService.sendEmail(saved.getEmail(), "🎉 Welcome to ParticipateHere – Account Created!", welcomeBody);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return AuthResponse.builder()
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String identifier = request.getEmail();

        // ADMIN path — only checks admins table
        if ("ADMIN".equalsIgnoreCase(request.getRole())) {
            Admin admin = adminRepository.findByUsername(identifier)
                    .orElseThrow(() -> new BadRequestException("Invalid Admin credentials."));

            if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
                throw new BadRequestException("Invalid Admin credentials.");
            }

            String accessToken = jwtTokenProvider.generateToken(admin.getUsername());
            setRefreshCookie(response, admin.getUsername());
            return buildAdminAuthResponse(admin, accessToken);
        }

        // STUDENT path
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));

        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new BadRequestException("Invalid credentials."));

        try {
            String loginBody =
                "<div style='font-family: Arial, sans-serif; max-width: 620px; margin: auto; padding: 0; border-radius: 10px; overflow: hidden; border: 1px solid #e0e0e0;'>" +
                "  <div style='background: linear-gradient(135deg, #1a73e8, #0d47a1); padding: 40px 30px; text-align: center;'>" +
                "    <h1 style='color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px;'>✅ Login Successful</h1>" +
                "    <p style='color: #c9d8ff; margin-top: 8px; font-size: 14px;'>You have successfully signed in to your account</p>" +
                "  </div>" +
                "  <div style='padding: 35px 30px; background: #ffffff;'>" +
                "    <p style='color: #333; font-size: 16px;'>Hello <strong>" + user.getFirstName() + " " + user.getLastName() + "</strong>,</p>" +
                "    <p style='color: #555; font-size: 15px; line-height: 1.7;'>" +
                "      You have successfully logged in to your <strong>ParticipateHere</strong> account. " +
                "      Welcome back! We're glad to see you again." +
                "    </p>" +
                "    <table style='width:100%; background:#f4f8ff; border-radius:8px; padding:15px; margin: 25px 0; border-left: 4px solid #1a73e8;'>" +
                "      <tr><td style='color:#888; padding:5px 10px; font-size:14px;'>📧 Account</td><td style='color:#333; font-weight:bold; font-size:14px;'>" + user.getEmail() + "</td></tr>" +
                "      <tr><td style='color:#888; padding:5px 10px; font-size:14px;'>🕐 Login Time</td><td style='color:#333; font-weight:bold; font-size:14px;'>" + java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19) + " IST</td></tr>" +
                "    </table>" +
                "    <p style='color:#e67e22; font-size:14px; background:#fff8ec; padding:12px 15px; border-radius:6px; border-left:4px solid #e67e22;'>" +
                "      ⚠️ If this wasn't you, please <strong>reset your password</strong> immediately and contact our support team." +
                "    </p>" +
                "  </div>" +
                "  <div style='background:#f5f5f5; padding:18px; text-align:center; border-top:1px solid #e0e0e0;'>" +
                "    <p style='color:#aaa; font-size:12px; margin:0;'>© 2025 ParticipateHere. All rights reserved. | This is an automated email, please do not reply.</p>" +
                "  </div>" +
                "</div>";

            emailService.sendEmail(user.getEmail(), "✅ Login Successful – ParticipateHere", loginBody);
        } catch (Exception e) {
            System.err.println("Failed to send login email: " + e.getMessage());
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        setRefreshCookie(response, user.getEmail());
        return buildAuthResponse(user, accessToken);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    public AuthResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(refreshToken);

        String username = refreshToken.getUsername();
        String newAccessToken = jwtTokenProvider.generateToken(username);

        // Rotate: delete old, issue new cookie
        setRefreshCookie(response, username);

        return adminRepository.findByUsername(username)
                .map(admin -> buildAdminAuthResponse(admin, newAccessToken))
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(username)
                            .orElseThrow(() -> new BadRequestException("User not found."));
                    return buildAuthResponse(user, newAccessToken);
                });
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    public void logout(String refreshTokenValue, HttpServletResponse response) {
        if (refreshTokenValue != null) {
            try {
                RefreshToken rt = refreshTokenService.findByToken(refreshTokenValue);
                refreshTokenService.deleteByUsername(rt.getUsername());
            } catch (Exception ignored) {
                // Token already gone — that's fine
            }
        }
        clearRefreshCookie(response);
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────
    private void setRefreshCookie(HttpServletResponse response, String username) {
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

        Cookie cookie = new Cookie("refresh_token", refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // change to true in production (HTTPS)
        cookie.setPath("/api/auth"); // only sent to auth endpoints
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ── Response builders ─────────────────────────────────────────────────────
    private AuthResponse buildAdminAuthResponse(Admin admin, String token) {
        return AuthResponse.builder()
                .token(token)
                .role("ADMIN")
                .userId(null)
                .email(admin.getUsername())
                .firstName("Super")
                .lastName("Admin")
                .studentId(null)
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .studentId(user.getStudentId())
                .build();
    }
}
