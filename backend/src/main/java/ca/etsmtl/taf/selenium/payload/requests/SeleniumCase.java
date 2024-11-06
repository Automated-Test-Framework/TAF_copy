package ca.etsmtl.taf.selenium.payload.requests;
import java.util.List;

import lombok.Data;

@Data
public class SeleniumCase {
    public int case_id;
    public String caseName;
    public List<SeleniumAction> actions;
}
