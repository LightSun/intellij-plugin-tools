package com.heaven7.plugin.intellij.builderclass;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.List;

public class BuiderProcessor {

    private final PsiClass mPsiClass;
    private final List<FieldInfo> mFieldInfos;
    private PsiField  mLastField;

    public BuiderProcessor(PsiClass psiClass) {
        this.mPsiClass = psiClass;
        this.mFieldInfos = new ArrayList<>();
    }

    public void parse() {
        PsiField[] fields = mPsiClass.getFields();
        if(fields.length == 0){
            return;
        }
        mLastField = fields[fields.length - 1];
        for(PsiField field : fields){
            PsiModifierList list = field.getModifierList();
            if(list != null){
                if(list.hasModifierProperty(PsiModifier.STATIC)){
                    continue;
                }
            }
            FieldInfo info = new FieldInfo();
            info.setField(field);
            info.setTypeTypeName(field.getType().getCanonicalText());

            final String name = field.getName();
            String nameForMethod  = name.substring(0,1).toUpperCase() + name.substring(1);
            if(name.startsWith("m")){
                if(name.length() >=2 && Character.isUpperCase(name.codePointAt(1))){
                    nameForMethod = name.substring(1);
                }
            }
            info.setSetMethodName("set" + nameForMethod);

            PsiType type = field.getType();
            if(type.equals(PsiType.BOOLEAN)){
                info.setGetMethodName("is" + nameForMethod);
            }else{
                info.setGetMethodName("get" + nameForMethod);
            }
            mFieldInfos.add(info);
        }
    }
    public void generate() {
        new WriteCommandAction.Simple(mPsiClass.getProject(), mPsiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                generate0();
            }
        }.execute();
    }

    private void generate0() {
        if(mFieldInfos.size()  == 0){
            return;
        }
        final Project project = mPsiClass.getProject();
        //delete if exist
        PsiClass psiClass_pre = JavaPsiFacade.getInstance(project).findClass(
                getBuilderClassName(mPsiClass), GlobalSearchScope.projectScope(project));
        if(psiClass_pre != null && psiClass_pre.isValid()){
            psiClass_pre.delete();
        }
       // Messages.showCheckboxOkCancelDialog()

        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        final PsiElement parent = mPsiClass;

        //create builder
        PsiTypeParameterList oldTypeList = mPsiClass.getTypeParameterList();
        PsiClass targetPsiClass = elementFactory.createClass("Builder");
        PsiModifierList modifierList = targetPsiClass.getModifierList();
        if(modifierList != null){
            modifierList.setModifierProperty(PsiModifier.STATIC, true);
        }
        replaceTypeParamsList(targetPsiClass, oldTypeList);
        //type String
        PsiTypeParameter[] parameters = targetPsiClass.getTypeParameters();
        String types = "";
        if(parameters.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<");
            for (int i = 0 , len = parameters.length ; i < len ; i ++ ) {
                PsiTypeParameter p  = parameters[i];
                sb.append(p.getName());
                if( i != len - 1){
                    sb.append(",");
                }
            }
            sb.append(">");
            types = sb.toString();
        }
        //fields
        for(FieldInfo info : mFieldInfos){
            styleManager.shortenClassReferences(targetPsiClass.addBefore(info.getField(), targetPsiClass.getLastChild()));
        }
        //methods
        for(FieldInfo info : mFieldInfos){
            PsiMethod method = elementFactory.createMethodFromText(
                    generateBuilderSet(info, targetPsiClass, types), targetPsiClass);
            styleManager.shortenClassReferences(targetPsiClass.addBefore(method, targetPsiClass.getLastChild()));
           /*
            //给方法加泛型
            PsiTypeParameterList parameterList = method.getTypeParameterList();
            if(parameterList != null && builderTypeParams != null){
                parameterList.replace(builderTypeParams);
            }*/
        }
        styleManager.shortenClassReferences(parent.addBefore(targetPsiClass, parent.getLastChild()));

        //====================================== create source for src PsiClass ======================================
        //create constructor for source PsiClass
        String baseConstructor = "protected "+ mPsiClass.getName() + " (" + getBuilderClassName(mPsiClass) + types + " builder)";

        StringBuilder sb = new StringBuilder();
        sb.append(baseConstructor);
        sb.append("{");
        for(FieldInfo info : mFieldInfos){
            sb.append("this.").append(info.getName())
                    .append(" = ")
                    .append("builder.").append(info.getName())
                    .append(";");
        }
        sb.append("}");
        PsiMethod constructor = elementFactory.createMethodFromText(sb.toString(), mPsiClass);
        //check constructor of src PsiClass
        PsiMethod preMethod = mPsiClass.findMethodBySignature(constructor, true);
        if(preMethod != null){
            preMethod.delete();
        }
        styleManager.shortenClassReferences(mPsiClass.addAfter(constructor, mLastField));
        //generate get method for src PsiClass
        for(FieldInfo info : mFieldInfos){
            PsiMethod method = elementFactory.createMethodFromText(generateGet(info), targetPsiClass);
            preMethod = mPsiClass.findMethodBySignature(method, true);
            if(preMethod != null){
                preMethod.delete();
            }
            styleManager.shortenClassReferences(mPsiClass.addBefore(method, targetPsiClass));
        }
        //Util.log(targetPsiClass.getQualifiedName(), Arrays.toString(parameters));
    }

    private String generateGet(FieldInfo info) {
        return String.format("%s %s(){ return this.%s ;}",
                info.getTypeTypeName(),
                info.getGetMethodName(),
                info.getName()
        );
    }

    private String generateBuilderSet(FieldInfo info, PsiClass targetPsiClass, String types) {
        String result = String.format("public %s %s(%s %s){\n " +
                        "this.%s = %s; return this; }",
                targetPsiClass.getQualifiedName() + types,
                info.getSetMethodName(),
                info.getTypeTypeName(),
                info.getName(),
                info.getName(),
                info.getName()
        );
        return result;
    }

    private static PsiElement replaceTypeParamsList(PsiClass psiClass, PsiTypeParameterList oldTypeParameterList) {
        final PsiTypeParameterList typeParameterList = psiClass.getTypeParameterList();
        assert typeParameterList != null;
        return typeParameterList.replace(oldTypeParameterList);
    }

    private String getBuilderClassName(PsiClass mPsiClass) {
        return mPsiClass.getQualifiedName() + ".Builder";
    }

}
