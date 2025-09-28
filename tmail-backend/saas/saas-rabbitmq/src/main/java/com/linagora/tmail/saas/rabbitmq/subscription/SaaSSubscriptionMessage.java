/********************************************************************
 *  As a subpart of Twake Mail, this file is edited by Linagora.    *
 *                                                                  *
 *  https://twake-mail.com/                                         *
 *  https://linagora.com                                            *
 *                                                                  *
 *  This file is subject to The Affero Gnu Public License           *
 *  version 3.                                                      *
 *                                                                  *
 *  https://www.gnu.org/licenses/agpl-3.0.en.html                   *
 *                                                                  *
 *  This program is distributed in the hope that it will be         *
 *  useful, but WITHOUT ANY WARRANTY; without even the implied      *
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR         *
 *  PURPOSE. See the GNU Affero General Public License for          *
 *  more details.                                                   *
 *******************************************************************/

package com.linagora.tmail.saas.rabbitmq.subscription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.linagora.tmail.rate.limiter.api.model.RateLimitingDefinition;

public record SaaSSubscriptionMessage(String internalEmail, Boolean isPaying, Boolean canUpgrade, SaasFeatures features) {
    public static class SaaSSubscriptionMessageParseException extends RuntimeException {
        SaaSSubscriptionMessageParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final Deserializer DESERIALIZER = new Deserializer();

    public static class Deserializer {
        public static SaaSSubscriptionMessage parseAMQPMessage(String messagePayload) {
            return parseAMQPMessage(messagePayload.getBytes(StandardCharsets.UTF_8));
        }

        public static SaaSSubscriptionMessage parseAMQPMessage(byte[] messagePayload) {
            try {
                return DESERIALIZER.deserialize(messagePayload);
            } catch (Exception e) {
                throw new SaaSSubscriptionMessageParseException("Failed to parse SaaS subscription message: " + new String(messagePayload, StandardCharsets.UTF_8), e);
            }
        }

        private final ObjectMapper objectMapper;

        public Deserializer() {
            this.objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        public SaaSSubscriptionMessage deserialize(byte[] jsonBytes) throws IOException {
            return objectMapper.readValue(jsonBytes, SaaSSubscriptionMessage.class);
        }
    }

    public record MailLimitation(
        @JsonProperty("storageQuota") Long storageQuota,
        @JsonProperty("mailsSentPerMinute") Long mailsSentPerMinute,
        @JsonProperty("mailsSentPerHour") Long mailsSentPerHour,
        @JsonProperty("mailsSentPerDay") Long mailsSentPerDay,
        @JsonProperty("mailsReceivedPerMinute") Long mailsReceivedPerMinute,
        @JsonProperty("mailsReceivedPerHour") Long mailsReceivedPerHour,
        @JsonProperty("mailsReceivedPerDay") Long mailsReceivedPerDay) {

        @JsonCreator
        public MailLimitation {
            Preconditions.checkNotNull(storageQuota, "storageQuota cannot be null");
            Preconditions.checkNotNull(mailsSentPerMinute, "mailsSentPerMinute cannot be null");
            Preconditions.checkNotNull(mailsSentPerHour, "mailsSentPerHour cannot be null");
            Preconditions.checkNotNull(mailsSentPerDay, "mailsSentPerDay cannot be null");
            Preconditions.checkNotNull(mailsReceivedPerMinute, "mailsReceivedPerMinute cannot be null");
            Preconditions.checkNotNull(mailsReceivedPerHour, "mailsReceivedPerHour cannot be null");
            Preconditions.checkNotNull(mailsReceivedPerDay, "mailsReceivedPerDay cannot be null");
        }

        public RateLimitingDefinition rateLimitingDefinition() {
            return RateLimitingDefinition.builder()
                .mailsSentPerMinute(mailsSentPerMinute)
                .mailsSentPerHours(mailsSentPerHour)
                .mailsSentPerDays(mailsSentPerDay)
                .mailsReceivedPerMinute(mailsReceivedPerMinute)
                .mailsReceivedPerHours(mailsReceivedPerHour)
                .mailsReceivedPerDays(mailsReceivedPerDay)
                .build();
        }
    }

    public record SaasFeatures(@JsonProperty("mail") MailLimitation mail) {
        @JsonCreator
        public SaasFeatures {
            Preconditions.checkNotNull(mail, "mail cannot be null");
        }
    }

    @JsonCreator
    public SaaSSubscriptionMessage(@JsonProperty("internalEmail") String internalEmail,
                                   @JsonProperty("isPaying") Boolean isPaying,
                                   @JsonProperty("canUpgrade") Boolean canUpgrade,
                                   @JsonProperty("features") SaasFeatures features) {
        Preconditions.checkNotNull(internalEmail, "internalEmail cannot be null");
        Preconditions.checkNotNull(isPaying, "isPaying cannot be null");
        Preconditions.checkNotNull(canUpgrade, "planName cannot be null");
        Preconditions.checkNotNull(features, "features cannot be null");

        this.internalEmail = internalEmail;
        this.isPaying = isPaying;
        this.canUpgrade = canUpgrade;
        this.features = features;
    }
}
