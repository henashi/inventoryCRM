package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.UserRepository;
import com.henashi.inventorycrm.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("用户不存在: {}", username);
                    return new UsernameNotFoundException("用户不存在: " + username);
                });

        log.info("成功加载用户: {} (角色: {})", username, user.getRole());

        return new CustomUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        log.debug("根据ID加载用户: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("用户不存在: ID={}", id);
                    return new UsernameNotFoundException("用户不存在: ID=" + id);
                });

        return new CustomUserDetails(user);
    }
}
