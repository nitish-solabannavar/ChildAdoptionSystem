package com.nitish.controller;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.nitish.dao.AdoptionRecordDAO;
import com.nitish.dao.ChildDAO;
import com.nitish.dao.RescueRecordDAO;
import com.nitish.dao.TreatmentRecordDAO;
import com.nitish.dao.UserDAO;
import com.nitish.pojo.Adopter;
import com.nitish.pojo.AdopterUserAccount;
import com.nitish.pojo.AdoptionRecord;
import com.nitish.pojo.Employee;
import com.nitish.pojo.EmployeeUserAccount;
import com.nitish.pojo.RescueRecord;

@Controller
//@RequestMapping("/user/*")
public class UserController  {

	@Autowired
	@Qualifier("userDAO")
	UserDAO userDAO;
	
	@Autowired
	@Qualifier("childDAO")
	ChildDAO childDAO;
	
	@Autowired
	@Qualifier("rescueRecordDAO")
	RescueRecordDAO rescueRecordDAO;
	
	@Autowired
	@Qualifier("treatmentRecordDAO")
	TreatmentRecordDAO treatmentRecordDAO;
	
	@Autowired
	@Qualifier("adoptionRecordDAO")
	AdoptionRecordDAO adoptionRecordDAO;
	
	@RequestMapping(value="/user/logout", method=RequestMethod.GET)
	public ModelAndView getLogout( HttpServletRequest request ) throws Exception{
		//ModelAndView mv = new ModelAndView();
		HttpSession session = (HttpSession) request.getSession();
		session.invalidate();
		return new ModelAndView("user_loginAsAdopter");
	}
	
	@RequestMapping(value="/user/signup",method = RequestMethod.GET)
	public ModelAndView getSignUp() throws Exception{
		//ModelAndView mv = new ModelAndView();
		return new ModelAndView("user_signup");
	}
	
	@RequestMapping(value="/user/signup",method = RequestMethod.POST)
	public ModelAndView postSignUp( HttpServletRequest request, @RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("password2") String password2, @RequestParam("lastname") String lastname,@RequestParam("firstname") String firstname,@RequestParam("gender") String gender,@RequestParam("address") String address,@RequestParam("phone") String phone) throws Exception{
		//System.out.println("Inside post method");;
		//ModelAndView mv = new ModelAndView();
		HttpSession session = (HttpSession) request.getSession();
		
		try{
			if(userDAO.isAdopterEmailValid(email)) {
				if(!password.equals(password2)) {
					return new ModelAndView("user_passwordDifferent");
				}
				else {
					Adopter adopter = userDAO.addAdopter(email, password, lastname, firstname, gender, address, phone);
					session.setAttribute("user", adopter);
					session.setAttribute("userType", "adopter");
					return new ModelAndView("adopter_dashboard");
				}
			}
			else {
				
				//return "user_emailNotValid";
				return new ModelAndView("user_emailInvalid");
			}
		}
		catch (Exception e)	{
			System.out.println("UserController_postSignUp: " + e);
		}
		return new ModelAndView("error");
	}
	
	@RequestMapping(value="/user/loginAsAdopter",method = RequestMethod.GET)
	public ModelAndView getLoginAsAdopter(){
		ModelAndView mv = new ModelAndView();
		return new ModelAndView("user_loginAsAdopter");
	}
	
	@RequestMapping(value="/user/loginAsEmployee", method = RequestMethod.GET)
	public ModelAndView getLoginAsEmployee(){
		ModelAndView mv = new ModelAndView();
		return new ModelAndView("user_loginAsEmployee");
	}
	
	@RequestMapping(value="/user/loginAsEmployee", method = RequestMethod.POST)
	public ModelAndView postLoginEmployee( HttpServletRequest request, @RequestParam("employeeid") long id, @RequestParam("password") String password ) throws Exception{
		//System.out.println("Inside controller method");
		ModelAndView mv = new ModelAndView();
		HttpSession session = (HttpSession) request.getSession();
				
		try{
			EmployeeUserAccount eua = userDAO.getEmployeeUserAccount(id, password);
			Employee e = userDAO.getEmployee(eua);
			System.out.println(e.getName());
			if(e != null) {
				session.setAttribute("user", e);
				if(e.getRole().equals("registrator")) {
					session.setAttribute("userType", "registrator");
					List<RescueRecord> recordList = rescueRecordDAO.getRescueRecordList();
					for(RescueRecord rr: recordList) {
						rr.setChild(childDAO.getChildById(rr.getChildid()));
						rr.setRegistrator(userDAO.getEmployeeByID(rr.getRegistratorid()));
					}
					session.setAttribute("rescueRecordList", recordList);
					mv.setViewName("registrator_workarea");
					mv.addObject("registrator", e);
					boolean isEmpty = false;
					if( recordList.size() == 0 ) {
						isEmpty = true;
					}
					mv.addObject("isEmpty", isEmpty);
					mv.addObject("message","No content.");
					return mv;
				}
				else if(e.getRole().equals("doctor")) {
					session.setAttribute("userType", "doctor");
					List<RescueRecord> recordList = rescueRecordDAO.getRescueRecordList();
					for(RescueRecord rr: recordList) {
						rr.setChild(childDAO.getChildById(rr.getChildid()));
						rr.setRegistrator(userDAO.getEmployeeByID(rr.getRegistratorid()));
					}
					session.setAttribute("rescueRecordList", recordList);
					mv.setViewName("doctor_workarea");
					boolean isEmpty = false;
					if( recordList.size() == 0 ) {
						isEmpty = true;
					}
					mv.addObject("isEmpty", isEmpty);
					mv.addObject("message","No content.");
					return mv;
				}
				else if(e.getRole().equals("adoption")) {
					session.setAttribute("userType", "adoption");
					mv.setViewName("adoption_workarea");
					//mv.addObject("adoption", e);
					//mv.addObject("adoptionRecordList", adoptionRecordDAO.getAdoptionRecordList());
					
					List<AdoptionRecord> recordList = adoptionRecordDAO.getAdoptionRecordListPending();
					for(AdoptionRecord r: recordList) {
						r.setChild(childDAO.getChildById(r.getChildId()));
						r.setAdopter(userDAO.getAdopterByEmail(r.getAdopterEmail()));
					}
					session.setAttribute("records", recordList);
					boolean isEmpty = false;
					if( recordList.size() == 0 ) {
						isEmpty = true;
					}
					mv.addObject("isEmpty", isEmpty);
					mv.addObject("message","No content.");
					return mv;
				}
				else if(e.getRole().equals("admin")) {
					session.setAttribute("userType", "admin");
					mv.setViewName("admin_report");
					mv.addObject("total", childDAO.getTotalCount());
					mv.addObject("age1Percentage", childDAO.getAge1Percentage());
					mv.addObject("age2Percentage", childDAO.getAge2Percentage());
					mv.addObject("otherPercentage", childDAO.getOtherPercentage());
					mv.addObject("malePercentage", childDAO.getMalePercentage());
					mv.addObject("femalePercentage", childDAO.getFemalePercentage());
					return mv;
				}
				else {
					System.out.println("Role exception.");
					return new ModelAndView("user_userNotFound");
				}
			}
		}
		catch (Exception e)	{
			System.out.println(e);
			mv.setViewName("user_userNotFound");
		}
		return mv;
		
	}
	
	@RequestMapping(value="/user/loginAsAdopter", method=RequestMethod.POST)
	public ModelAndView postLoginAdopter( HttpServletRequest request, @RequestParam("email") String email, @RequestParam("password") String password ) throws Exception{
		ModelAndView mv = new ModelAndView();
		HttpSession session = (HttpSession) request.getSession();
				
		try{	
			AdopterUserAccount aua = userDAO.getAdopterUserAccount(email, password);
			Adopter a = userDAO.getAdopter(aua);
			if(a != null) {
				System.out.println("a is not null.");
				mv.setViewName("adopter_dashboard");
				session.setAttribute("user", a);
				session.setAttribute("userType", "adopter");
				mv.addObject("adopter", a);
			}
			else {
				//user not found
				System.out.println("User not found.");
				return new ModelAndView("user_userNotFound");
			}
		}
		catch (Exception e)	{
			System.out.println(e);
			mv.setViewName("user_userNotFound");
		}
		return mv;
	}
	
	@RequestMapping(value="/",method = RequestMethod.GET)
	public String home(Locale locale, Model model){
		return "login";
	}
}