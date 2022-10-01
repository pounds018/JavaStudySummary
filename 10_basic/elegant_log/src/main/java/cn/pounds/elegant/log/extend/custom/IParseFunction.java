package cn.pounds.elegant.log.extend.custom;

public interface IParseFunction {

  default boolean executeBefore(){
    return false;
  }

  String functionName();

  String apply(Object value);
}