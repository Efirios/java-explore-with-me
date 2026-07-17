package ru.practicum.ewm.user;

import java.util.List;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;

public interface UserService {

    UserDto registerUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void delete(Long userId);
}
