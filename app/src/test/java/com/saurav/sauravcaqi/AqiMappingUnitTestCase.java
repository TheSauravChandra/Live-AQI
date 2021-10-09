package com.saurav.sauravcaqi;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.saurav.sauravcaqi.utils.MyUtils.getAQIcolourMappingRes;
import static junit.framework.TestCase.assertEquals;

@RunWith(value = Parameterized.class)
public class AqiMappingUnitTestCase {
  
  @Parameterized.Parameter(value = 0)
  public String tcName;
  
  @Parameterized.Parameter(value = 1)
  public Integer aqi;
  
  @Parameterized.Parameter(value = 2)
  public Integer output;
  
  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"TC0", -12, R.color.purple_700},
        {"TC1", 20, R.color.aqi_good},
        {"TC2", 90, R.color.aqi_satisfactory},
        {"TC3", 110, R.color.aqi_moderate},
        {"TC4", 220, R.color.aqi_poor},
        {"TC5", 330, R.color.aqi_very_poor},
        {"TC6", 440, R.color.aqi_severe},
        {"TC7", 1000, R.color.purple_700}
    });
  }
  
  @Test
  public void validateAqiColorUnitTC() {
    assertEquals(getAQIcolourMappingRes(aqi), output.longValue());
  }
  
}
