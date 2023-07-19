package com.bulletjournal.ledger;

import com.bulletjournal.controller.models.Label;
import com.bulletjournal.controller.models.Transaction;
import com.bulletjournal.controller.utils.ZonedDateTimeHelper;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class LedgerSummaryCalculator {

    public static class LedgerSummaryRequest {
        private LedgerSummaryType ledgerSummaryType;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private List<Transaction> transactions;
        private FrequencyType frequencyType;

        public LedgerSummaryRequest(LedgerSummaryType ledgerSummaryType, ZonedDateTime startTime, ZonedDateTime endTime, List<Transaction> transactions, FrequencyType frequencyType) {
            this.ledgerSummaryType = ledgerSummaryType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.transactions = transactions;
            this.frequencyType = frequencyType;
        }

        public LedgerSummaryType getLedgerSummaryType() {
            return ledgerSummaryType;
        }

        public void setLedgerSummaryType(LedgerSummaryType ledgerSummaryType) {
            this.ledgerSummaryType = ledgerSummaryType;
        }

        public ZonedDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(ZonedDateTime startTime) {
            this.startTime = startTime;
        }

        public ZonedDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(ZonedDateTime endTime) {
            this.endTime = endTime;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        public FrequencyType getFrequencyType() {
            return frequencyType;
        }

        public void setFrequencyType(FrequencyType frequencyType) {
            this.frequencyType = frequencyType;
        }
    }

    public LedgerSummary getLedgerSummary(LedgerSummaryRequest request) {
        final LedgerSummary ledgerSummary = new LedgerSummary(request.getTransactions(),
                ZonedDateTimeHelper.getDate(request.getStartTime()),
                ZonedDateTimeHelper.getDate(request.getEndTime()));

        final Total total = new Total();
        Map<String, Transactions> m = new HashMap<>();

        Function<? super TransactionsSummary, ? extends String> transactionsSummariesComparator =
                TransactionsSummary::getName;

        switch (request.getLedgerSummaryType()) {
            case DEFAULT:
                transactionsSummariesComparator = TransactionsSummary::getMetadata;
                final Function<Transaction, String> name;
                final Function<Transaction, String> metadata;
                switch (request.getFrequencyType()) {
                    case MONTHLY:
                        name = (t) -> t.getReadableYearMonth();
                        metadata = (t) -> t.getYearMonth();
                        break;
                    case YEARLY:
                        name = (t) -> t.getYear();
                        metadata = (t) -> t.getYear();
                        break;
                    case WEEKLY:
                        name = (t) -> t.getReadableWeek();
                        metadata = (t) -> t.getWeek();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }

                processTransaction(request.getTransactions(), total, (t -> {
                    double amount = t.getAmount();
                    Transactions tran = m.computeIfAbsent(name.apply(t), k -> new Transactions());
                    tran.setMeta(metadata.apply(t));
                    switch (TransactionType.getType(t.getTransactionType())) {
                        case INCOME:
                            tran.addIncome(amount);
                            break;
                        case EXPENSE:
                            tran.addExpense(amount);
                            break;
                    }
                }));
                break;
            case LABEL:
                processTransaction(request.getTransactions(), total, (t -> {
                    double amount = t.getAmount();
                    for (Label l : t.getLabels()) {
                        Transactions tran = m.computeIfAbsent(l.getValue(), k -> new Transactions());
                        switch (TransactionType.getType(t.getTransactionType())) {
                            case INCOME:
                                tran.addIncome(amount);
                                break;
                            case EXPENSE:
                                tran.addExpense(amount);
                                break;
                        }
                    }
                }));
                break;
            case PAYER:
                processTransaction(request.getTransactions(), total, (t -> {
                    Transactions tran = m.computeIfAbsent(t.getPayer().getName(), k -> new Transactions());
                    double amount = t.getAmount();
                    switch (TransactionType.getType(t.getTransactionType())) {
                        case INCOME:
                            tran.addIncome(amount);
                            break;
                        case EXPENSE:
                            tran.addExpense(amount);
                            break;
                    }
                }));
                break;
            default:
                throw new IllegalArgumentException("Invalid LedgerSummaryType " + request.getLedgerSummaryType());
        }

        ledgerSummary.setIncome(total.totalIncome);
        ledgerSummary.setExpense(total.totalExpense);
        ledgerSummary.setBalance(total.totalIncome - total.totalExpense);
        final List<TransactionsSummary> transactionsSummaries = new ArrayList<>();
        m.forEach((k, v) -> {
            double balance = v.getIncome() - v.getExpense();
            transactionsSummaries.add(new TransactionsSummary(
                    k,
                    v.getMeta(),
                    v.getIncome(),
                    Math.round((v.getIncome() * 100 / total.totalIncome) * 100.0) / 100.0,
                    v.getExpense(),
                    Math.round(v.getExpense() * 100 / total.totalExpense * 100.0) / 100.0,
                    balance,
                    Math.round(balance * 100 / ledgerSummary.getBalance() * 100.0) / 100.0,
                    v.getIncomeCount(),
                    v.getExpenseCount()
            ));
        });
        transactionsSummaries.sort(Comparator.comparing(transactionsSummariesComparator));
        ledgerSummary.setTransactionsSummaries(transactionsSummaries);
        return ledgerSummary;
    }


    private void processTransaction(List<Transaction> transactions, Total total,
                                    Consumer<Transaction> transactionHandler) {
        for (Transaction t : transactions) {
            transactionHandler.accept(t);
            TransactionType transactionType = TransactionType.getType(t.getTransactionType());
            double amount = t.getAmount();

            switch (transactionType) {
                case INCOME:
                    total.totalIncome += amount;
                    break;
                case EXPENSE:
                    total.totalExpense += amount;
                    break;
            }
        }
    }

    private static class Transactions {
        double income = 0.0;
        double expense = 0.0;
        int incomeCount = 0;
        int expenseCount = 0;
        String meta = null;

        void addIncome(double amount) {
            this.income += amount;
            this.incomeCount++;
        }

        void addExpense(double amount) {
            this.expense += amount;
            this.expenseCount++;
        }

        public double getIncome() {
            return income;
        }

        public double getExpense() {
            return expense;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public int getIncomeCount() {
            return incomeCount;
        }

        public int getExpenseCount() {
            return expenseCount;
        }
    }

    private static class Total {
        double totalIncome = 0.0;
        double totalExpense = 0.0;
    }
}
