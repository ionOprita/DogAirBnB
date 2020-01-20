package application.basicControllers;

import application.database.repositories.LoginRepository;
import application.services.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Created by thanasis on 23/8/2017.
 */

@Controller
public class GeneralUserControlers {

    @Autowired
    RegisterService registerService;

    @Autowired
    LoginRepository loginRepository;
    @RequestMapping(value = "/login",method = RequestMethod.GET)
    String getLogin(@RequestParam Map<String,String> allParams,
                    @AuthenticationPrincipal final UserDetails userDetails,
                    Model model
    ){
        System.out.println(allParams);
        System.out.println(allParams.get("error"));

        if(userDetails!=null && allParams.get("logout")==null ){
            System.out.println(userDetails);
            return "redirect:/";
        }
        model.addAttribute("urlParams",allParams);
        System.out.println("ook come login");
        return "login";
    }


    @RequestMapping(value = "/register",method = RequestMethod.POST)
    String postRegister(Model model,
                       @RequestParam Map<String,String> allParams,
                       @RequestParam(name = "photo")MultipartFile photo,
                       @AuthenticationPrincipal final UserDetails userDetails
    ){
        if(userDetails!=null){
            return "redirect:/";
        }
        try {
            registerService.createLogin(allParams,photo);
           // registerService.createLogin(allParams);
        }catch (Exception e){
            if(e.getMessage().equals("User Exists")){
                model.addAttribute("error","user-exists");
                model.addAttribute("oldVal",allParams);
                return "register";
            }else if(e.getMessage().equals("Passwords do not match")){
                model.addAttribute("oldVal",allParams);
                model.addAttribute("error","password-match");
                return "register";
            }else{
                model.addAttribute("error","other");
                model.addAttribute("oldVal",allParams);
                e.printStackTrace();
                return "register";
            }
        }
        System.out.println("Done");
        return  "redirect:login?login=successful";
    }

    @RequestMapping("/register")
    String getRegister(Model model,
                       @RequestParam Map<String,String> allParams,
                       @AuthenticationPrincipal final UserDetails userDetails
    ){
        if(userDetails!=null){
            return "redirect:/";
        }
        return "register";
    }

    @RequestMapping(value = "/profile",method = RequestMethod.GET)
    String getProfile(Model model,
                      @AuthenticationPrincipal final UserDetails userDetails
    ){
        if(userDetails==null){
            return "redirect:/login";
        }
        if( userDetails.getAuthorities().iterator().next().getAuthority().contains("admin") ){
        //if he is an admin he can only be admin!!! nothing else
            return "redirect:/profile/admin";
        }
        model.addAttribute("user",loginRepository.findOne(userDetails.getUsername()));
        return "profile";
    }

    @RequestMapping(value = "/profile",method = RequestMethod.POST)
    String editProfile(Model model,
                       @AuthenticationPrincipal final UserDetails userDetails,
                       @RequestParam(name = "photo")MultipartFile photo,
                       @RequestParam Map<String,String> allParams
    ){
        System.out.println("Yeeeezzz");
        if(userDetails==null){
            return "redirect:/login";
        }
        model.addAttribute("user",loginRepository.findOne(userDetails.getUsername()));
        if(photo.getSize()==0){
            photo=null;
        }
        try {
            registerService.editLogin(
                    userDetails.getUsername(),
                    allParams.get("name"),
                    allParams.get("surname"),
                    allParams.get("telephone"),
                    allParams.get("email"),
                    allParams.get("password"),
                    allParams.get("confirm"),
                    photo
                    );
        } catch (Exception e) {
            model.addAttribute("error","pass_confirm");
            e.printStackTrace();
        }
        return "profile";
    }

}
