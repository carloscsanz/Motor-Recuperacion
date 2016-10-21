/* Realizado por Carlos Contreras Sanz (100303562) y Miguel Xoel García Balsa (100291036) */
package modeloVectorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import edu.smu.tspell.wordnet.*;

public class Sinonimos {

	public Map<String, Double> encontrarSinonimos(Map<String, Double> query){

		WordNetDatabase database = WordNetDatabase.getFileInstance();
		System.setProperty("wordnet.database.dir", "./dict/");
		ArrayList<String> querySinonimosAux = new ArrayList<String>();
		Map<String, Double> querySinonimos = new HashMap<String, Double>();

		for (Entry<String, Double> entry : query.entrySet()){

			querySinonimos.put(entry.getKey(), (double)entry.getValue());
			Synset[] synsets = database.getSynsets(entry.getKey(), SynsetType.NOUN);

			if (synsets.length > 0){

				for (int i = 0; i < synsets.length; i++){

					String[] wordForms = synsets[i].getWordForms();

					for (int j = 0; j < wordForms.length; j++){

						String [] aux = wordForms[j].split(" ");

						if(aux.length == 1){
							if(!querySinonimosAux.contains(wordForms[j])){

								querySinonimosAux.add(wordForms[j]);
							}
						}

					}

				}
			}

		}
		if(querySinonimosAux!=null && !querySinonimosAux.isEmpty()){
			double peso = ((double)1/querySinonimosAux.size());
			for (int i = 0 ; i<querySinonimosAux.size() ; i++){
				if(!querySinonimos.containsKey(querySinonimosAux.get(i))){
					querySinonimos.put(querySinonimosAux.get(i), peso);
				}
			}
		}
				
		List<String> filtered = querySinonimos.keySet()
				.stream()
				.filter(p -> !p.equals(p.toLowerCase()))
				.collect(Collectors.toList());
		
		for(int i=0;i<filtered.size();i++){
			querySinonimos.put(filtered.get(i).toLowerCase(), querySinonimos.get(filtered.get(i))/2 );
		}
		
		return querySinonimos;

	}

}


