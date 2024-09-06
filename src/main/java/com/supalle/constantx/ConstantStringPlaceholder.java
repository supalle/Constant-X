package com.supalle.constantx;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量字符串占位显示
 * <p>原理是使用IDEA的代码块展开和折叠操作</p>
 * <p>默认打开文件时会显示字符串常量的值</p>
 * <p>如果没有显示，可以配合快捷键：`Ctrl + -` 、`Ctrl + +` </p>
 * <p>或者 `Ctrl + Shift + -` 、`Ctrl + Shift + +` 来配合显示/隐藏操作</p>
 *
 * @author Supalle
 */
public class ConstantStringPlaceholder extends FoldingBuilderEx {
    private static final String STRING_TYPE_CANONICAL_TEXT = String.class.getCanonicalName();

    /**
     * 构建折叠区域
     */
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        // 递归访问Java代码；PSI解析需要添加 plugins.set(listOf("com.intellij.java"))
        root.accept(new JavaRecursiveElementVisitor() {

            // 常量都是 PsiReferenceExpression 类型，所以只需要关注该类型
            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                // 获取引用表达式的类型，只关注字符串类型的常量
                PsiType type = expression.getType();
                if (type == null || !STRING_TYPE_CANONICAL_TEXT.equals(type.getCanonicalText())) {
                    return;
                }
                // 判断是否为常量
                if (PsiUtil.isConstantExpression(expression)) {
                    // 构建折叠描述信息
                    FoldingDescriptor foldingDescriptor = new FoldingDescriptor(expression, expression.getTextRange());
                    // 添加折叠后的占位符
                    foldingDescriptor.setPlaceholderText(getPlaceholderText(expression.getNode()));
                    descriptors.add(foldingDescriptor);
                }
            }
        });
        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    /**
     * 获取占位符文本
     */
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        PsiElement expression = node.getPsi();
        // 计算常量的具体值
        Object constValue = JavaPsiFacade.getInstance(expression.getProject()).getConstantEvaluationHelper().computeConstantExpression(expression);
        if (constValue instanceof String stringValue) {
            // 最多显示100个字符，超出末尾为...
            if (stringValue.length() > 100) {
                stringValue = stringValue.substring(0, 100) + StringUtil.THREE_DOTS;
            }
            return "\"" + stringValue + "\"";
        }
        return null;
    }

    /**
     * 是否默认收缩：即为true则默认显示占位符文本
     */
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return getPlaceholderText(node) != null;
    }

    /**
     * 是否默认收缩：即为true则默认显示占位符文本
     */
    @Override
    public boolean isCollapsedByDefault(@NotNull FoldingDescriptor foldingDescriptor) {
        return foldingDescriptor.getPlaceholderText() != null;
    }
}
