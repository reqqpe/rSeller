package my.reqqpe.rseller.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class Parse {

    public IntList StringListToIntList(List<String> list) {
        IntList result = new IntArrayList();
        for (String str : list) {
            if (str.contains("-")) {
                String[] parts = str.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else {
                result.add(Integer.parseInt(str));
            }
        }
        return result;
    }
}
