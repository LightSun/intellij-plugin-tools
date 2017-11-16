package com.heaven7.plugin.intellij.builderclass;

import com.intellij.psi.PsiField;

public class FieldInfo {

    private PsiField field;
    private String setMethodName;
    private String getMethodName;
    private String typeTypeName;

    public String getName(){
        return field.getName();
    }

    public PsiField getField() {
        return field;
    }

    public void setField(PsiField field) {
        this.field = field;
    }
    public String getTypeTypeName() {
        return typeTypeName;
    }

    public void setTypeTypeName(String typeTypeName) {
        this.typeTypeName = typeTypeName;
    }

    public String getSetMethodName() {
        return setMethodName;
    }

    public void setSetMethodName(String setMethodName) {
        this.setMethodName = setMethodName;
    }

    public String getGetMethodName() {
        return getMethodName;
    }

    public void setGetMethodName(String getMethodName) {
        this.getMethodName = getMethodName;
    }
}
