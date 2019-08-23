package nl.clockwork.ebms.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class Utils
{
	public static List<Integer> getIntegerList(String input)
	{
		return Arrays.stream(StringUtils.split(input,',')).map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
	}
}
