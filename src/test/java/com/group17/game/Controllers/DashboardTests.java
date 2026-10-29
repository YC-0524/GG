package com.group17.game.Controllers;

import com.group17.game.Models.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DashboardTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    // TODO: actual dashboard tests, API tests aren't dashboard tests
}
