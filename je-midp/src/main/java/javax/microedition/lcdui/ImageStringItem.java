package javax.microedition.lcdui;


class ImageStringItem extends Item
{

	Image img;
  StringComponent stringComponent;


  public ImageStringItem(String label, Image img, String text)
  {
    super(label);
		stringComponent = new StringComponent(text);
    setImage(img);
  }


	public Image getImage()
	{
    return img;
  }
    
    
	public void setImage(Image img)
	{
    this.img = img;
		if (this.img != null) {
			stringComponent.setWidthDecreaser(img.getWidth() + 2);
		}
	}


	public String getText()
	{
		return stringComponent.getText();
	}


	public void setText(String text)
	{
		stringComponent.setText(text);
	}
	

	int getHeight()
	{
		if (img != null && img.getHeight() > stringComponent.getHeight()) {
			return img.getHeight();
		} else {
			return stringComponent.getHeight();
		}
	}


  void invertPaint(boolean state)
  {
    stringComponent.invertPaint(state);
  }


  int paint(Graphics g)
  {
		if (stringComponent == null) {
			return 0;
		}

		if (img != null) {
			g.drawImage(img, 0, 0, Graphics.LEFT | Graphics.TOP);
			g.translate(img.getWidth() + 2, 0);
		}

		int y = stringComponent.paint(g);

		if (img != null) {
			g.translate(-img.getWidth() - 2, 0);
		}

		return y;
  }

}
