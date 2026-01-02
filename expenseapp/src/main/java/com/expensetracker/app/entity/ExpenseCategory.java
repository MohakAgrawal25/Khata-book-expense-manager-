package com.expensetracker.app.entity;

public enum ExpenseCategory {
    HOUSING("Housing", CategoryType.ESSENTIAL),
    TRANSPORTATION("Transportation", CategoryType.ESSENTIAL),
    FOOD("Food", CategoryType.ESSENTIAL),
    UTILITIES("Utilities", CategoryType.ESSENTIAL),
    HEALTHCARE("Healthcare", CategoryType.ESSENTIAL),
    INSURANCE("Insurance", CategoryType.ESSENTIAL),
    ENTERTAINMENT("Entertainment", CategoryType.DISCRETIONARY),
    PERSONAL_CARE("Personal Care", CategoryType.DISCRETIONARY),
    SHOPPING("Shopping", CategoryType.DISCRETIONARY),
    TRAVEL("Travel", CategoryType.DISCRETIONARY),
    DINING_OUT("Dining Out", CategoryType.DISCRETIONARY),
    SAVINGS("Savings", CategoryType.FINANCIAL_GOAL),
    INVESTMENTS("Investments", CategoryType.FINANCIAL_GOAL),
    EDUCATION("Education", CategoryType.INVESTMENT),
    DEBT_PAYMENTS("Debt Payments", CategoryType.FINANCIAL_GOAL),
    GIFTS_DONATIONS("Gifts & Donations", CategoryType.DISCRETIONARY),
    SUBSCRIPTIONS("Subscriptions", CategoryType.ESSENTIAL),
    PET_CARE("Pet Care", CategoryType.ESSENTIAL),
    CHILD_CARE("Child Care", CategoryType.ESSENTIAL),
    VEHICLE_MAINTENANCE("Vehicle Maintenance", CategoryType.ESSENTIAL),
    HOME_MAINTENANCE("Home Maintenance", CategoryType.ESSENTIAL),
    OTHER("Other", CategoryType.OTHER);

    private final String displayName;
    private final CategoryType type;

    ExpenseCategory(String displayName, CategoryType type) {
        this.displayName = displayName;
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CategoryType getType() {
        return type;
    }

    public boolean isEssential() {
        return type == CategoryType.ESSENTIAL;
    }

    public boolean isDiscretionary() {
        return type == CategoryType.DISCRETIONARY;
    }

    public boolean isFinancialGoal() {
        return type == CategoryType.FINANCIAL_GOAL;
    }

    public enum CategoryType {
        ESSENTIAL, DISCRETIONARY, FINANCIAL_GOAL, INVESTMENT, OTHER
    }
}