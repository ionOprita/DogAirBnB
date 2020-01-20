package application.basicControllers;

import application.database.Apartment;
import application.database.BookInfo;
import application.database.repositories.ApartmentRepository;
import application.recommended.Recommendation;
import application.search.Result;
import application.search.Search;
import application.search.SearchService;
import application.services.AvailabilityService;
import application.services.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by thanasis on 2/8/2017.
 */
@Controller
public class HomePage {
    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    SearchService searchService;

    @Autowired
    RegisterService registerService;

    @Autowired
    Recommendation recommendation;

    SimpleDateFormat dateFormat=new SimpleDateFormat("MM/dd/yyy");
    {
        dateFormat=new SimpleDateFormat("MM/dd/yyy");
    }

    @RequestMapping("/")
        String homeController(Model model
    ){
        return "index";
    }

    @RequestMapping("/result")
    String resultController(Model model,
                            @RequestParam("date-range") String dateRange,
                            @RequestParam("city") String city,
                            @RequestParam("people") Integer people,
                            @RequestParam(value = "max-cost",required = false,defaultValue = "0") Integer maxCost,
                            @RequestParam(value = "wifi",required = false, defaultValue = "false") Boolean wifi,
                            @RequestParam(value = "fridge",required = false, defaultValue = "false") Boolean fridge,
                            @RequestParam(value = "kitchen",required = false, defaultValue = "false") Boolean kitchen,
                            @RequestParam(value = "tv",required = false, defaultValue = "false") Boolean tv,
                            @RequestParam(value = "parking",required = false, defaultValue = "false") Boolean parking,
                            @RequestParam(value = "elevator",required = false, defaultValue = "false") Boolean elevator,
                            @RequestParam(value = "air-condition",required = false, defaultValue = "false") Boolean airCondition,
                            @RequestParam(value = "page",required = false, defaultValue = "1") Integer pageNum,
                            @RequestParam(value = "roomType" ,required=false,defaultValue = "")String roomType,
                            @AuthenticationPrincipal final UserDetails userDetails
    ){
        Date fromDate=null;
        Date toDate=null;
        String[] splitStr = dateRange.split("-");
        try {
            if(splitStr[0]!=null && splitStr[0]!=""){
                fromDate = dateFormat.parse(splitStr[0]);
            }else{
                return "index";
            }

            if(splitStr[1]!=null && splitStr[1]!=""){
                toDate = dateFormat.parse(splitStr[1]);
            }else{
                return "index";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "index";
        }
        System.out.println("model = [" + model + "], dateRange = [" + dateRange + "], city = [" + city + "], people = [" + people + "], maxCost = [" + maxCost + "], wifi = [" + wifi + "], fridge = [" + fridge + "], kitchen = [" + kitchen + "], tv = [" + tv + "], parking = [" + parking + "], elevator = [" + elevator + "], airCondition = [" + airCondition + "], pageNum = [" + pageNum + "], roomType = [" + roomType + "]");

        Search filters = new Search(fromDate,toDate,city,people,wifi,fridge,kitchen,tv,parking,elevator,airCondition,roomType,maxCost);
        Result searchResults = searchService.getResultList(filters,pageNum);
        if(userDetails!=null && registerService.isUser(userDetails.getUsername())){
            //if we have a new search from a user
            System.out.println("Updating the search thing");
            recommendation.newSearch(userDetails.getUsername(),filters);
        }
        model.addAttribute("results",searchResults);

        model.addAttribute("oldDateStr",dateRange);
        model.addAttribute("oldValues",filters);
        return "result_page";
    }

    @RequestMapping("/apartment")
    String hotePageController(Model model,
                              @RequestParam(name = "hotel-id") int hotelId,
                              @RequestParam(name = "date-range",required = false,defaultValue = "") String dateRange,
                              @RequestParam(name = "people",required = false,defaultValue = "1") Integer people,
                              @RequestParam(name = "book-failed",required = false,defaultValue = "1") String bookFailed
    ){
        Date from = null;
        Date to = null;
        System.out.println("model = [" + model + "], hotelId = [" + hotelId + "], dateRange = [" + dateRange + "]");
        System.out.println(dateRange);
//        get the apertment
        Apartment apartment = apartmentRepository.findOne(hotelId);
        if(apartment==null){
            System.out.println("No apartment with id "+hotelId);
            return "redirect:/";
        }
        model.addAttribute("apartment",apartment);

//        check if the apartment is available on this date
        if(dateRange!=null && !dateRange.trim().equals("")){
            String buff[] = dateRange.split("-") ;
            try {
                from = dateFormat.parse(buff[0]);
                to = dateFormat.parse(buff[1]);
            }catch (Exception e){
                e.printStackTrace();
                return "redirect:/";
            }
            model.addAttribute("dateRange",dateRange);

            if(availabilityService.checkAvailability(apartment,from,to)){
                model.addAttribute("hotelIsBusy",false);
            }else{
                model.addAttribute("hotelIsBusy",true);
            }
        }
        if(bookFailed.equals("true")){
            model.addAttribute("hotelIsBusy",true);
        }
        model.addAttribute(people);
        return "hotel_page";
    }
}
