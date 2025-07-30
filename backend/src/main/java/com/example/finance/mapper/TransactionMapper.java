package com.example.finance.mapper;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Wallet;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {
    @Mapping(source = "user.id",      target = "userId")
    @Mapping(source = "wallet.id",    target = "walletId")
    @Mapping(source = "category.id",  target = "categoryId")
    @Mapping(source = "date",         target = "date")
    @Mapping(source = "filePath",     target = "filePath")
    @Mapping(source = "status",       target = "status")
    @Mapping(source = "tags",         target = "tags")
    @Mapping(source = "deleted",    target = "isDeleted")
    @Mapping(source = "deletedAt",    target = "deletedAt")
    @Mapping(source = "createdAt",    target = "createdAt")
    @Mapping(source = "updatedAt",    target = "updatedAt")
    TransactionDTO toDto(Transaction entity);

    @InheritConfiguration
    Transaction toEntity(TransactionDTO dto);

    @Named("walletFromId")
    default Wallet walletFromId(Long id) {
        if (id == null) return null;
        Wallet w = new Wallet();
        w.setId(id);
        return w;
    }

    @Named("categoryFromId")
    default Category categoryFromId(Long id) {
        if (id == null) return null;
        Category c = new Category();
        c.setId(id);
        return c;
    }
}
