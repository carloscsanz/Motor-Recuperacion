/* Realizado por Carlos Contreras Sanz (100303562) y Miguel Xoel García Balsa (100291036) */
package modeloVectorial;

import java.util.Comparator;
import java.util.Map;

public class OrdenarHashMap implements Comparator<String> {
	
	    private Map<String, Double> base;

	    public OrdenarHashMap(Map<String, Double> base) {
	        this.base = base;
	    }

	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	    }

}
