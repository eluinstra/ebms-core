package nl.clockwork.ebms.common.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Utils
{
	public static List<Integer> getIntegerList(String input)
	{
		List<Integer> result = new ArrayList<>();
		if (!StringUtils.isEmpty(input))
		{
			String[] strings = input.split(",");
			for (String s : strings)
				result.add(Integer.parseInt(s.trim()));
		}
		return result;
	}
}
