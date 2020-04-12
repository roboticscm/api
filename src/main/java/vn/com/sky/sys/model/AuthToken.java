package vn.com.sky.sys.model;

import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import vn.com.sky.base.GenericEntity;

@EqualsAndHashCode(callSuper = false)
@Data
@Table
public class AuthToken extends GenericEntity {

	private Boolean authorized = false;

}
