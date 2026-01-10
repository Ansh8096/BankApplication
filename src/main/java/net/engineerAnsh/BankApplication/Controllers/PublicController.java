package net.engineerAnsh.BankApplication.Controllers;

import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private UserService userService;

    @GetMapping("/health-check")
    public String applicationStatus(){
        return "ok";
    }

    @PutMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user){
        try {
            userService.saveNewUser(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error occurred while saving the new user ", e);
            return ResponseEntity.badRequest().build();
        }
    }

//    @PutMapping("/login")
//    public ResponseEntity<User> loginTheUser(@RequestBody User user){
//        mpp.put(user, user.getName());
//        return new ResponseEntity<>(user,HttpStatus.CREATED);
//    }


}
