package com.harumi.petrecord.user;

import com.harumi.petrecord.auth.dto.UserSummary;
import com.harumi.petrecord.security.CurrentUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserSummary getMe(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return UserSummary.from(user);
    }
}
