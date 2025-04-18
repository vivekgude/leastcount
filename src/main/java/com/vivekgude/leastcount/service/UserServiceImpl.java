package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dao.User;
import com.vivekgude.leastcount.model.dto.UserDTO;
import com.vivekgude.leastcount.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Override
    public User createUser(UserDTO userDTO) {
        User user = new User(userDTO.getEmailId(), userDTO.getName(), userDTO.getPassword());
        return userRepository.save(user);
    }
}
