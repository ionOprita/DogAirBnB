package application.basicControllers;

import application.database.*;
import application.database.repositories.ApartmentRepository;
import application.database.repositories.ChatRepository;
import application.database.repositories.ImageRepository;
import application.database.repositories.LoginRepository;
import application.services.ApartmentService;
import application.services.AvailabilityService;
import application.services.BookService;
import application.services.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by thanasis on 23/8/2017.
 */
@Controller
public class HostController {
    SimpleDateFormat dateFormat=new SimpleDateFormat("MM/dd/yyy");

    @Autowired
    private ChatRepository chatRepository;

    {
        dateFormat=new SimpleDateFormat("MM/dd/yyy");
    }

    @Autowired
    AvailabilityService availabilityService;
    @Autowired
    LoginRepository loginRepository;
    @Autowired
    ApartmentService apartmentService;
    @Autowired
    private ApartmentRepository apartmentRepository;
    @Autowired
    private BookService bookService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @RequestMapping(value = "/apartment/book",method = RequestMethod.POST)
    String bookApartment(Model model,
                         @RequestParam Map<String,String> allParams,
                         @AuthenticationPrincipal final UserDetails userDetails
    ){
        if(userDetails==null){
            return "redirect:/login";
        }
        System.out.println(allParams.toString());
        String buff[] = allParams.get("dates").split("-");
        try {
            bookService.createBookInfo(
                    Integer.parseInt(allParams.get("apartment-id")),
                    userDetails.getUsername(),
                    buff[0],
                    buff[1],Integer.parseInt(allParams.get("Adults")));
        } catch (Exception e) {
            if(e.getMessage().equals("The apartment is not available")){
                model.addAttribute("hotelIsBusy",true);
                return  "redirect:/apartment?hotel-id="+allParams.get("apartment-id")+"&book-failed=true";
            }
            e.printStackTrace();
            return "redirect:/";
        }
        model.addAttribute("book","done");
        return "redirect:/profile?book=done";
    }
    @RequestMapping(value = "/profile/host/add_apartment",method = RequestMethod.POST)
    String postAddApartmentController(Model model,
                                      @ModelAttribute Apartment formApartment,
                                      @RequestParam(name = "image1",required = false)MultipartFile image1,
                                      @RequestParam(name = "image2",required = false)MultipartFile image2,
                                      @RequestParam(name = "image3",required = false)MultipartFile image3,
                                      @RequestParam(name = "image4",required = false)MultipartFile image4,
                                      @AuthenticationPrincipal final UserDetails userDetails

    ){
        System.out.println( formApartment.toString() );
        System.out.println("Creating Hotel " +formApartment.getName());
        System.out.println("-----------");

        System.out.println(image1);
        System.out.println(image1.getSize());
        System.out.println(image1.getOriginalFilename());
        System.out.println("-----------");

        System.out.println(image2);
        System.out.println(image2.getSize());
        System.out.println(image2.getOriginalFilename());
        System.out.println("-----------");

        System.out.println(image3);
        System.out.println(image3.getSize());
        System.out.println(image3.getOriginalFilename());
        System.out.println("-----------");

        System.out.println(image4);
        System.out.println(image4.getSize());
        System.out.println(image4.getOriginalFilename());
        System.out.println("-----------");
        if(image1.getSize()==0){
            image1=null;
        }
        if(image2.getSize()==0){
            image2=null;
        }
        if(image3.getSize()==0){
            image3=null;
        }
        if(image4.getSize()==0){
            image4=null;
        }
        try {
            apartmentService.createApartment(loginRepository.findOne(userDetails.getUsername()),formApartment,image1,image2,image3,image4);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
        return "redirect:/profile";
    }

    @RequestMapping(value = "/profile/host/add_apartment",method = RequestMethod.GET)
    String getAddApartmentController(Model model,
                                     @AuthenticationPrincipal final UserDetails userDetails
    ){
        Apartment apartment = new Apartment();
        model.addAttribute("apartment",apartment);
        return "/add_apartment";
    }

    @RequestMapping(value = "/profile/host/apartments",method = RequestMethod.GET)
    String getApartmetns(Model model,
                         @AuthenticationPrincipal final UserDetails userDetails
    ){
        if(userDetails==null){
            return "redirect:/login";
        }
        List<Apartment> apartments = loginRepository.findOne(userDetails.getUsername()).getApartments();

//        we add this just to use the form for update(just in case)
        Apartment apartment = new Apartment();
        model.addAttribute("apartment",apartment);

        model.addAttribute("apartments",apartments);
        return "apartments";
    }

    @RequestMapping(value = "/profile/host/apartment/dates/{apartment_id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Availability> getExistingDates(@AuthenticationPrincipal final UserDetails userDetails,
                                        @PathVariable(value = "apartment_id") int apartmentId

    ){
        if(userDetails==null){
            System.out.println("it's null");
            return null;
        }else {
            System.out.println(userDetails.getUsername());
        }
        Apartment apartment = apartmentRepository.findOne(apartmentId);
        if(apartment==null){
            System.out.println("No apartment with id "+apartmentId);
            return null;
        }
        System.out.println(apartment.getAvailabilities().size());
        return apartment.getAvailabilities();
    }

    @RequestMapping(value = "profile/host/apartment/dates/{apartment_id}",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Boolean addDate(@AuthenticationPrincipal final UserDetails userDetails,
                   @PathVariable(value = "apartment_id") int apartmentId,
                   @RequestParam("date_range") String dateStr
    ){
        if(userDetails==null){
            return false;
        }
        System.out.println("userDetails = [" + userDetails + "], apartmentId = [" + apartmentId + "], dateStr = [" + dateStr + "]");
        Date from = null;
        Date to = null;
        if(dateStr==null || dateStr.trim().equals("")){
            System.out.println("Date not exists");
        }
        String buff[] = dateStr.split("-");
        try{
            from=dateFormat.parse(buff[0]);
            to=dateFormat.parse(buff[1]);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        try{
            Apartment apartment = apartmentRepository.findOne(apartmentId);
            Availability availability = new Availability(from,to,apartment);
            if( !apartment.getLogin().getUsername().equals(userDetails.getUsername()) ){
                System.out.println("not yours hotel "+ userDetails.getUsername());
                return false;
            }
            availabilityService.createAvailability(apartment,availability);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @RequestMapping(value = "/profile/host/apartment/{apartment_id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Apartment getApartmentInfo(@AuthenticationPrincipal final UserDetails userDetails,
                                @PathVariable(value = "apartment_id") int apartmentId

    ){
        Apartment apartment = apartmentRepository.findOne(apartmentId);
        if(apartment==null){
            System.out.println("No apartment with id "+apartmentId);
            return null;
        }
        return apartment;
    }

    @RequestMapping(value = "/profile/host/apartment/{apartment_id}",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Boolean editApartmentInfo(@AuthenticationPrincipal final UserDetails userDetails,
                              @PathVariable(value = "apartment_id") int apartmentId,
                              @ModelAttribute Apartment formApartment
    ){
        System.out.println("-----------------------");
        System.out.println(formApartment.toString());
        try{
            if(apartmentService.editApartment(formApartment.getApartmentId(),formApartment)==false){
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        System.out.println("-----------------------");
        return true;
    }

    @RequestMapping(value = "/profile/host/apartment/images/{apartment_id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Image> getImagesInfo(@AuthenticationPrincipal final UserDetails userDetails,
                               @PathVariable(value = "apartment_id") int apartmentId

    ){
        Apartment apartment = apartmentRepository.findOne(apartmentId);
        if(apartment==null){
            System.out.println("No apartment with id "+apartmentId);
            return null;
        }
        List<Image> images=apartment.getImages();
        System.out.println("YEa editing photos");
        return images;
    }

    @RequestMapping(value = "/profile/host/images")
    String saveImage(@AuthenticationPrincipal final UserDetails userDetails,
                      @RequestParam(name="photo") MultipartFile photo,
                      @RequestParam(name= "apartmentId") int apartmentId
    ){
        if(photo.getSize()==0){
            System.out.println("None an image");
        }
        System.out.println("Ready to save an image");
        Apartment apartment=apartmentRepository.findOne(apartmentId);
        try{
            fileUploadService.save_image(photo,apartment);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "redirect:/profile/host/apartments";
    }

    @RequestMapping(value = "/profile/host/delete_image",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    boolean saveImage(@AuthenticationPrincipal final UserDetails userDetails,
                     @RequestParam(name= "photoId") int photoId
    ){

        System.out.println("Ready to delete an Image");
        Image imageToDel = imageRepository.findOne(photoId);
        if(imageToDel==null){
            return false;
        }
        try{
            Path pathFile = null;
            Resource file = null;
            pathFile= Paths.get("ApartmentPhotos/");

            File deleteImage;
            String filename=imageToDel.getPicturePath().replaceAll("ApartmentPhotos/","");
            file=new UrlResource(pathFile.resolve(filename).toUri());

            deleteImage=file.getFile();
            deleteImage.delete();

            imageRepository.delete(photoId);
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @RequestMapping(value = "/profile/host/apartment/chats/{apartment_id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Chat> getApartmentChat(@AuthenticationPrincipal final UserDetails userDetails,
                                @PathVariable(value = "apartment_id") int apartmentId
    ){
        if(apartmentService.authentication(userDetails,apartmentId)==false){
            System.out.println("not your apartment "+userDetails.toString()+"|||"+apartmentId);
            return null;
        }
        Apartment apartment = apartmentRepository.findOne(apartmentId);
        if(apartment==null){
            return null;
        }
        return apartment.getChats();
    }

    @RequestMapping(value = "/profile/host/chats/messages/{chat_id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Message> getChatMessages(@AuthenticationPrincipal final UserDetails userDetails,
                                   @PathVariable(value = "chat_id") int chatId
    ){
        Chat chat=chatRepository.findOne(chatId);
        if(chat==null){
            return null;
        }
        if(apartmentService.authentication(userDetails,chat.getApartment())==false){
            System.out.println("not your chat "+userDetails.toString()+"|||"+chatId);
            return null;
        }
        return chat.getMessages();
    }

    @RequestMapping(value = "/profile/host/chats/messages/{chat_id}",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Boolean newChatMessage(@AuthenticationPrincipal final UserDetails userDetails,
                           @PathVariable(value = "chat_id") int chatId,
                           @RequestParam("message") String message
    ){
        Chat chat=chatRepository.findOne(chatId);
        if(chat==null){
            return false;
        }
        if(apartmentService.authentication(userDetails,chat.getApartment())==false){
            System.out.println("not your chat "+userDetails.toString()+"|||"+chatId);
            return false;
        }

        if(apartmentService.newMessageFormHost(message,chatId)==false){
            System.out.println("the service not OK");
            return false;
        }else{
            return true;
        }

    }

}
