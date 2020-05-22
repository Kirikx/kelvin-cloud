package com.kirikomp.common;


import java.util.List;


public class DeleteFilesCommand
		extends DataPackage
{

  private final List<String> filenames;


  public DeleteFilesCommand(List<String> filenames)
  {
	this.filenames = filenames;
  }


  public List<String> getFileNames()
  {
	return filenames;
  }

}