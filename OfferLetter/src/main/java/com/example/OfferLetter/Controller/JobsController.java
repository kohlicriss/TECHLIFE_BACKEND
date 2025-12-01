package com.example.OfferLetter.Controller;


//import com.example.payroll.Repository.JobsRepository;
import com.example.OfferLetter.Service.JobsService;
import com.example.OfferLetter.Entity.Jobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/jobs")
public class JobsController {

   @Autowired
    private JobsService jobsService;

   @PostMapping("/create")
    public Jobs createJob(@RequestBody Jobs jobs){
       return  jobsService.createJobs(jobs);
   }

   @GetMapping("/getall")
    public List<Jobs> getJobs(){
       return jobsService.getAllJobs();
   }

   @PutMapping("/{id}/update")
    public Jobs editJobs( @PathVariable  Long id , @RequestBody Jobs jobs){
       return jobsService.editJobDetails(id , jobs);
   }

   @DeleteMapping("/{id}/delete")
    public String deleteJob(@PathVariable Long id){
       jobsService.deleteJob(id);
       return "Job with ID " + id + " deleted successfully.";
   }




}
