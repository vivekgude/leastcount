package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dao.User;
import com.vivekgude.leastcount.model.dto.UserDTO;

public interface UserService {
    User createUser(UserDTO user);

}
