package com.vanniktech.dependency.graph.generator.dot

/** http://www.graphviz.org/doc/info/shapes.html */
enum class Shape(val string: String) {
  BOX("box"),
  POLYGON("polygon"),
  ELLIPSE("ellipse"),
  OVAL("oval"),
  CIRCLE("circle"),
  POINT("point"),
  EGG("egg"),
  TRIANGLE("triangle"),
  PLAINTEXT("plaintext"),
  PLAIN("plain"),
  DIAMOND("diamond"),
  TRAPEZIUM("trapezium"),
  PARALLELOGRAM("parallelogram"),
  HOUSE("house"),
  PENTAGON("pentagon"),
  HEXAGON("hexagon"),
  SEPTAGON("septagon"),
  OCTAGON("octagon"),
  DOUBLECIRCLE("doublecircle"),
  DOUBLEOCTAGON("doubleoctagon"),
  TRIPLEOCTAGON("tripleoctagon"),
  INVTRIANGLE("invtriangle"),
  INVTRAPEZIUM("invtrapezium"),
  INVHOUSE("invhouse"),
  MDIAMOND("Mdiamond"),
  MSQUARE("Msquare"),
  MCIRCLE("Mcircle"),
  RECT("rect"),
  RECTANGLE("rectangle"),
  SQUARE("square"),
  STAR("star"),
  NONE("none"),
  UNDERLINE("underline"),
  CYLINDER("cylinder"),
  NOTE("note"),
  TAB("tab"),
  FOLDER("folder"),
  BOX3D("box3d"),
  COMPONENT("component"),
  PROMOTER("promoter"),
  CDS("cds"),
  TERMINATOR("terminator"),
  UTR("utr"),
  PRIMERSITE("primersite"),
  RESTRICTIONSITE("restrictionsite"),
  FIVEPOVERHANG("fivepoverhang"),
  THREEPOVERHANG("threepoverhang"),
  NOVERHANG("noverhang"),
  ASSEMBLY("assembly"),
  SIGNATURE("signature"),
  INSULATOR("insulator"),
  RIBOSITE("ribosite"),
  RNASTAB("rnastab"),
  PROTEASESITE("proteasesite"),
  PROTEINSTAB("proteinstab"),
  RPROMOTER("rpromoter"),
  RARROW("rarrow"),
  LARROW("larrow"),
  LPROMOTER("lpromoter");

  override fun toString() = string
}
