package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.UserCreateDTO;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.mapper.UserMapper;
import com.henashi.inventorycrm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {

    private final UserService userService;

    private final UserMapper userMapper;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/{id}")
    @Operation(summary = "根据 ID 查询用户")
    public UserDTO getUser(
            @PathVariable @NotNull @Min(1) Long id) {
        return userService.findUserDTOById(id);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查询用户")
    public UserDTO getUserByUsername(
            @PathVariable @NotNull String username) {
        return userService.findByUsername(username);
    }

    @PostMapping
    @Operation(summary = "创建新用户",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户信息",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserCreateDTO.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "用户创建成功"),
                    @ApiResponse(responseCode = "400", description = "请求参数错误")
            }
    )
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody UserCreateDTO userCreateDTO) {
        UserDTO savedUser = userService.saveUser(userCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedUser.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "根据 ID 更新用户")
    public UserDTO updateUser(
            @PathVariable @NotNull @Min(1) Long id,
            @Valid @RequestBody UserCreateDTO userCreateDTO) {
        return userService.updateUser(id, userCreateDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "根据 ID 删除用户")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @NotNull @Min(1) Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST request for registering user
     * @param userCreateDTO 用户信息
     * @return register or login page
     */
    @Operation(summary = "注册新用户")
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute UserCreateDTO userCreateDTO, BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        logger.info("Post register request received");
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).reduce((item1, item2) -> item1 + ", " + item2).orElse("Validation failed");
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/register?error";
        }
        try {
            userService.registerUser(userMapper.createToEntity(userCreateDTO));
        }
        catch (Exception userAlreadyExists) {
            return "redirect:/register?error";
        }
        return "redirect:/login/success";
    }
}