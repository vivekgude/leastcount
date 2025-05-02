package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.ApiResponse;
import com.vivekgude.leastcount.model.dao.User;
import com.vivekgude.leastcount.model.dto.UserDTO;
import com.vivekgude.leastcount.service.UserService;
import com.vivekgude.leastcount.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.vivekgude.leastcount.constants.Constants.*;

@Slf4j
@RestController
public class UserController extends BaseController {

    @Autowired
    UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            User user = userService.createUser(userDTO);
            log.info("User created {}", user);
            return ResponseEntity.ok(ResponseUtil.success(true, USER_CREATED, user));
        } catch (Exception e) {
            log.error("Exception occured in createUser", e);
            return ResponseEntity.internalServerError()
                    .body(ResponseUtil.error(false, "Error processing the request"));
        }
    }

}
