class Complex {
    final double x, y;
  
    Complex(double x, double y) {
        this.x = x;
        this.y = y;
    }
  
    double getModulus() {
        return Math.hypot(x, y);
    }
  
    double getX() {
        return x;
    }
  
    double getY() {
        return y;
    }
}
class Use {
    void test(Complex c) {
        System.out.println(c.getM<caret>odulus());
    }
}