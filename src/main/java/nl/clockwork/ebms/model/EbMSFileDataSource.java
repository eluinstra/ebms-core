package nl.clockwork.ebms.model;

import java.io.File;

import javax.activation.FileDataSource;

public class EbMSFileDataSource extends FileDataSource
{
	private String name;
	private String contentType;

	public EbMSFileDataSource(String name, String contentType, File file)
	{
		super(file);
		this.name = name;
		this.contentType = contentType;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getContentType()
	{
		return contentType;
	}
}
