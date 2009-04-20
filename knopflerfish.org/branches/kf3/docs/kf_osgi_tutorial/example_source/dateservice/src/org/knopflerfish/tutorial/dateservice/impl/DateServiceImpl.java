package org.knopflerfish.tutorial.dateservice.impl; 

import java.text.DateFormat; 
import java.util.Date; 
import org.knopflerfish.tutorial.dateservice.DateService;


public class DateServiceImpl implements DateService { 
  public String getFormattedDate(Date date) { 
    return DateFormat.getDateInstance(DateFormat.SHORT)
      .format(date); 
  } 
} 
