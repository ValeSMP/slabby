package com.valesmp.slabby.audit;

import java.util.Date;
import java.util.UUID;

public interface Auditable {

    Date createdOn();

    void createdOn(final Date createdOn);

//    UUID createdBy();
//    void createdBy(final UUID uniqueId);

    Date lastModifiedOn();

    void lastModifiedOn(final Date lastModifiedOn);

//    UUID lastModifiedBy();
//    void lastModifiedBy(final UUID uniqueId);

}
