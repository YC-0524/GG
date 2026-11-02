package com.group17.game.Commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.AccessService;
import com.group17.game.Utilities.HashUtil;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(MockitoExtension.class)
class UserCommandsTest {

    @Mock
    AccessService accessService;

    @Autowired
    UserRepository userRepo;

    @InjectMocks
    UserCommands userCommands;

    @Test
    void userUpgradeReturnsServiceResult() {
        User user = new User("Test", "test@test.com", HashUtil.generateRandomHash());
        userRepo.save(user);

        assertTrue(userRepo.existsById(user.getId()));
        assertTrue(user.getRole() == UserRole.USER);

        when(accessService.upgrade(1L, "ADMIN")).thenReturn("User 1 is now ADMIN");

        String result = userCommands.userUpgrade(1L, "ADMIN");
        assertThat(result).isEqualTo("User 1 is now ADMIN");
        verify(accessService).upgrade(1L, "ADMIN");
    }
}
