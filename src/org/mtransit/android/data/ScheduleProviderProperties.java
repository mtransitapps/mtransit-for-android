package org.mtransit.android.data;

public class ScheduleProviderProperties {

	private String targetAuthority;
	private String authority;

	public ScheduleProviderProperties(String authority, String targetAuthority) {
		this.authority = authority;
		this.targetAuthority = targetAuthority;
	}

	public String getTargetAuthority() {
		return this.targetAuthority;
	}

	public String getAuthority() {
		return authority;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(ScheduleProviderProperties.class.getSimpleName()).append('{') //
				.append("authority:").append(authority).append(',') //
				.append("targetAuthority:").append(targetAuthority).append(',') //
				.append('}').toString();
	}

}
