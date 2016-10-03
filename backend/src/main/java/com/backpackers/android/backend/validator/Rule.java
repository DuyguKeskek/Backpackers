package com.backpackers.android.backend.validator;

import com.google.api.server.spi.ServiceException;

public interface Rule<E extends ServiceException> {

    void validate() throws E;
}
