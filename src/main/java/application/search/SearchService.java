package application.search;

import application.database.Apartment;
import application.services.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.persistence.EntityManager;
import java.util.*;
import javax.persistence.Query;

/**
 * Created by thanasis on 16/8/2017.
 */
@Service
public class SearchService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    AvailabilityService availabilityService;


// page : current page from the result list (STARTS FROM 1)
    public Result getResultList(Search search,int page){
        String queryStr = search.buildQuery();
        Query query=entityManager.createNativeQuery(queryStr,Apartment.class);
        search.passParameter(queryStr,query);
        List<Apartment> apartmentsResult = query.getResultList();
        System.out.println(apartmentsResult.size()+" results");

        List<Apartment> apartmentsResultsFinal=new ArrayList<Apartment>();
        for (Apartment oneApartment :
                apartmentsResult) {
            if(availabilityService.checkAvailability(oneApartment,search.getFromDate(),search.getToDate())){
                apartmentsResultsFinal.add(oneApartment);
            }
        }
        Collections.sort(apartmentsResultsFinal, new Comparator<Apartment>() {
            @Override
            public int compare(Apartment o1, Apartment o2) {
                return Double.compare(o1.getPrice(), o2.getPrice());
            }
        });
        System.out.println("\nend of results\n");
        Result searchResults = new Result(apartmentsResultsFinal,page);
        return searchResults;

    }
}
