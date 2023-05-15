package it.jorge.protectora.Controller;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.jorge.protectora.Model.BaseResponse;
import it.jorge.protectora.Model.Pet;
import it.jorge.protectora.Model.RequestAdoption;
import it.jorge.protectora.Model.User;
import it.jorge.protectora.Service.PetService;
import it.jorge.protectora.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static it.jorge.protectora.util.Constants.route;
import static it.jorge.protectora.util.Functions.*;

@RestController
@Tag(name = "Common Controller")
@RequestMapping("/api")
public class CommonController {

    @Autowired
    UserService userService;
    @Autowired
    PetService petService;


    @Operation(summary = "Pet Adoption")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet's adoption realized", content = @Content(schema = @Schema(implementation = Pet.class))),
            @ApiResponse(responseCode = "404", description = "Error to adoption", content = @Content(schema = @Schema(implementation = Pet.class)))
    })

    @PostMapping("demand")
    public ResponseEntity<BaseResponse> insertAdoption(@RequestHeader("Authorization") String token, @RequestBody RequestAdoption request) {

        try {
            Claims claims = getClaims(token);
            Pet pet = petService.findById(request.getIdPet()).get();
            User user = userService.findById((Integer) claims.get("id")).get();

            if (pet.isAdoption()) {

                return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseController.ERROR1);

            }
            if (user.getPhone().isEmpty()) {

                return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseController.ERROR2);

            }

            if (!validateDniNie(request.getIdentification())){

                return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseController.ERROR3);

            }

            if (!validateDate(request.getBornDate())){

                return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseController.ERROR4);


            }

            List<Pet> pets = user.getPets();
            pets.add(pet);
            user.setPets(pets);


            List<User> users = pet.getUsers();
            users.add(user);
            pet.setUsers(users);
            request.setEmail(user.getEmail());
            request.setPhone(user.getPhone());
            InformGenerate(request, pet);
            petService.save(pet);
            userService.save(user);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseController.Ok);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseController.ERROR5);
        }


    }
    @PostMapping("/user/pets")
    public List<Pet> getUserPets(@RequestHeader("Authorization") String jwt) {
        System.out.printf(jwt);
        Claims claims = getClaims(jwt);
        User user = userService.findById((Integer) claims.get("id")).get();
        return user.getPets();
    }

    @GetMapping("image/{img}")
    public ResponseEntity<?> getImage (@PathVariable String img){
        if(img!= null && !img.isEmpty()){
            try {
                Path file = Paths.get(route+ "\\" + img);
                byte[] buffer = Files.readAllBytes(file);
                ByteArrayResource byteArrayResource = new ByteArrayResource(buffer);
                return  ResponseEntity.ok()
                        .contentLength(buffer.length)
                        .contentType(MediaType.parseMediaType("image/png"))
                        .body(byteArrayResource);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
