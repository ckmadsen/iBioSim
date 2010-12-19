package gcm2sbml.gui.modelview.movie.visualizations;

import java.awt.Color;

public class ColorScheme {

	private double min;
	private double max;
	private ColorGradient colorGradient;
	
	public ColorGradient getColorGradient() {
		return colorGradient;
	}

	public void setColorGradient(ColorGradient colorGradient) {
		this.colorGradient = colorGradient;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
		assert(min < max);
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
		assert(min < max);
	}
	
	/**
	 * Set min and max simultaneously to prevent
	 * assertion errors if you set them out of order.
	 * @param min
	 * @param max
	 */
	public void setMinMax(double min, double max){
		this.min = min;
		this.max = max;
		assert(min < max);
	}

	public ColorScheme() {
		// TODO Auto-generated constructor stub
		this.min=0;
		this.max=1;
		this.colorGradient = new ColorGradient();
	}

	/**
	 * Returns a color on the gradient based on the input value.
	 * @param val: The value you want to get a color for.
	 * @return: A color on the gradient. RETURNS RED IF min >= max!
	 */
	public Color getColor(double val){
		// an error case that would mess up the math.
		// Note that we could have also set the ratio to 0.0
		// and return a color on the gradient, but I thought that
		// setting it to red would more quickly alert the user
		// that they did something wrong.
		if(min >= max)
			return Color.RED;
		
		float ratio = (float)((val - min) / (max - min));
		
		// Note that ColorGradient properly handles the case when the ratio
		// is outside the bounds of [0,1]
		return this.colorGradient.getIntermediateColor(ratio);
	}
}
